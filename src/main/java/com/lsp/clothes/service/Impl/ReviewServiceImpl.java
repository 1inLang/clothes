package com.lsp.clothes.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.mapper.DesignFileMapper;
import com.lsp.clothes.mapper.ReviewRecordMapper;
import com.lsp.clothes.mapper.UserMapper;
import com.lsp.clothes.model.dto.review.ReviewActionRequest;
import com.lsp.clothes.model.dto.task.TaskQueryRequest;
import com.lsp.clothes.model.entity.DesignFile;
import com.lsp.clothes.model.entity.DesignTask;
import com.lsp.clothes.model.entity.ReviewRecord;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.enums.ReviewResultEnum;
import com.lsp.clothes.model.enums.TaskStatusEnum;
import com.lsp.clothes.model.vo.ReviewDetailVO;
import com.lsp.clothes.model.vo.ReviewRecordVO;
import com.lsp.clothes.model.vo.TaskVO;
import com.lsp.clothes.service.DesignFileService;
import com.lsp.clothes.service.DesignTaskService;
import com.lsp.clothes.service.NotificationService;
import com.lsp.clothes.service.ReviewService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewServiceImpl extends CrudRepository<ReviewRecordMapper, ReviewRecord>
        implements ReviewService {

    @Resource private DesignTaskService designTaskService;
    @Resource private DesignFileService designFileService;
    @Resource private DesignFileMapper designFileMapper;
    @Resource private UserMapper userMapper;
    @Resource private NotificationService notificationService;

    @Override
    public Page<TaskVO> pendingPage(TaskQueryRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        request.setStatus(TaskStatusEnum.PENDING_REVIEW.getValue());
        request.setReviewerId(loginUser.getId());
        Page<DesignTask> page = designTaskService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                designTaskService.getQueryWrapper(request, loginUser, false));
        return designTaskService.getTaskVOPage(page);
    }

    @Override
    public ReviewDetailVO getDetail(Long taskId, User loginUser) {
        DesignTask task = requireReviewer(taskId, loginUser, false);
        ReviewDetailVO detail = new ReviewDetailVO();
        detail.setTask(designTaskService.getTaskVO(task));
        if (task.getSubmittedFileId() != null) {
            detail.setSubmittedFile(designFileService.getFileVO(task.getSubmittedFileId(), loginUser));
        }
        detail.setHistory(getHistory(taskId, loginUser));
        return detail;
    }

    @Override
    public List<ReviewRecordVO> getHistory(Long taskId, User loginUser) {
        designTaskService.getAccessibleTask(taskId, loginUser);
        return this.list(new QueryWrapper<ReviewRecord>()
                        .eq("task_id", taskId).orderByDesc("create_time", "id"))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(ReviewActionRequest request, User loginUser) {
        review(request, loginUser, ReviewResultEnum.APPROVED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(ReviewActionRequest request, User loginUser) {
        review(request, loginUser, ReviewResultEnum.REJECTED);
    }

    private void review(ReviewActionRequest request, User loginUser, ReviewResultEnum result) {
        validateRequest(request, result);
        ReviewRecord existing = this.getOne(new QueryWrapper<ReviewRecord>()
                .eq("request_no", request.getRequestNo()).last("LIMIT 1"));
        if (existing != null) {
            ThrowUtils.throwIf(!existing.getTaskId().equals(request.getTaskId())
                            || !existing.getReviewerId().equals(loginUser.getId())
                            || !existing.getResult().equals(result.getValue()),
                    ErrorCode.OPERATION_ERROR, "幂等请求号已被其他审核操作使用");
            return;
        }

        DesignTask task = requireReviewer(request.getTaskId(), loginUser, true);
        ThrowUtils.throwIf(task.getSubmittedFileId() == null,
                ErrorCode.OPERATION_ERROR, "任务没有已提交的设计稿版本");
        DesignFile file = designFileMapper.selectById(task.getSubmittedFileId());
        ThrowUtils.throwIf(file == null || !Integer.valueOf(1).equals(file.getSubmittedFlag()),
                ErrorCode.OPERATION_ERROR, "提交审核的设计稿版本不存在");
        ThrowUtils.throwIf(!file.getVersionNo().equals(request.getVersionNo()),
                ErrorCode.OPERATION_ERROR, "设计稿版本已变化，请刷新后重试");

        boolean approved = result == ReviewResultEnum.APPROVED;
        designTaskService.reviewTask(task.getId(), request.getTaskVersion(), approved,
                request.getOpinion(), loginUser);

        ReviewRecord record = new ReviewRecord();
        record.setTaskId(task.getId());
        record.setFileId(file.getId());
        record.setVersionNo(file.getVersionNo());
        record.setReviewerId(loginUser.getId());
        record.setResult(result.getValue());
        record.setOpinion(StrUtil.trim(request.getOpinion()));
        record.setRequestNo(request.getRequestNo().trim());
        ThrowUtils.throwIf(!this.save(record), ErrorCode.OPERATION_ERROR, "保存审核记录失败");
        notificationService.send(task.getAssigneeId(), "review",
                approved ? "设计审核已通过" : "设计稿被退回修改",
                approved ? "“" + task.getTaskName() + "”已通过审核。"
                        : "“" + task.getTaskName() + "”被退回："
                        + StrUtil.blankToDefault(StrUtil.trim(request.getOpinion()), "请按审核要求修改"),
                "task", task.getId(), "/tasks", "review-result:" + request.getRequestNo());
    }

    private DesignTask requireReviewer(Long taskId, User loginUser, boolean pendingOnly) {
        DesignTask task = designTaskService.getAccessibleTask(taskId, loginUser);
        ThrowUtils.throwIf(task.getReviewerId() == null || !task.getReviewerId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只有任务指定审核人可以查看或执行审核");
        if (pendingOnly) {
            ThrowUtils.throwIf(!TaskStatusEnum.PENDING_REVIEW.getValue().equals(task.getStatus()),
                    ErrorCode.OPERATION_ERROR, "只有待审核任务可以审核");
        }
        return task;
    }

    private void validateRequest(ReviewActionRequest request, ReviewResultEnum result) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null
                        || request.getTaskVersion() == null || request.getVersionNo() == null,
                ErrorCode.PARAMS_ERROR, "任务、任务版本和设计稿版本不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getRequestNo())
                        || request.getRequestNo().length() > 64
                        || !request.getRequestNo().matches("^[A-Za-z0-9_-]+$"),
                ErrorCode.PARAMS_ERROR, "幂等请求号格式不合法");
        if (result == ReviewResultEnum.REJECTED) {
            ThrowUtils.throwIf(StrUtil.isBlank(request.getOpinion()),
                    ErrorCode.PARAMS_ERROR, "退回修改时审核意见不能为空");
        }
        ThrowUtils.throwIf(request.getOpinion() != null && request.getOpinion().length() > 1000,
                ErrorCode.PARAMS_ERROR, "审核意见不能超过 1000 个字符");
    }

    private ReviewRecordVO toVO(ReviewRecord record) {
        ReviewRecordVO vo = new ReviewRecordVO();
        vo.setId(String.valueOf(record.getId())); vo.setTaskId(String.valueOf(record.getTaskId()));
        vo.setFileId(String.valueOf(record.getFileId())); vo.setVersionNo(record.getVersionNo());
        vo.setReviewerId(String.valueOf(record.getReviewerId())); vo.setResult(record.getResult());
        vo.setOpinion(record.getOpinion()); vo.setRequestNo(record.getRequestNo());
        vo.setCreateTime(record.getCreateTime());
        User reviewer = userMapper.selectById(record.getReviewerId());
        if (reviewer != null) vo.setReviewerName(reviewer.getUserName());
        return vo;
    }
}
