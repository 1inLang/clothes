package com.lsp.clothes.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.BusinessException;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.mapper.DesignProjectMapper;
import com.lsp.clothes.mapper.DesignFileMapper;
import com.lsp.clothes.mapper.DesignTaskMapper;
import com.lsp.clothes.mapper.ProjectMemberMapper;
import com.lsp.clothes.mapper.UserMapper;
import com.lsp.clothes.model.dto.task.TaskAddRequest;
import com.lsp.clothes.model.dto.task.TaskAssignRequest;
import com.lsp.clothes.model.dto.task.TaskProgressRequest;
import com.lsp.clothes.model.dto.task.TaskQueryRequest;
import com.lsp.clothes.model.dto.task.TaskUpdateRequest;
import com.lsp.clothes.model.entity.DesignProject;
import com.lsp.clothes.model.entity.DesignFile;
import com.lsp.clothes.model.entity.DesignTask;
import com.lsp.clothes.model.entity.ProjectMember;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.enums.ProjectMemberRoleEnum;
import com.lsp.clothes.model.enums.DesignFileTypeEnum;
import com.lsp.clothes.model.enums.ProjectPriorityEnum;
import com.lsp.clothes.model.enums.ProjectStatusEnum;
import com.lsp.clothes.model.enums.TaskStatusEnum;
import com.lsp.clothes.model.vo.TaskVO;
import com.lsp.clothes.service.DesignProjectService;
import com.lsp.clothes.service.DesignTaskService;
import com.lsp.clothes.service.NotificationService;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DesignTaskServiceImpl extends CrudRepository<DesignTaskMapper, DesignTask>
        implements DesignTaskService {

    @Resource
    private DesignProjectService designProjectService;
    @Resource
    private DesignFileMapper designFileMapper;
    @Resource
    private DesignProjectMapper designProjectMapper;
    @Resource
    private ProjectMemberMapper projectMemberMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserService userService;

    @Resource
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTask(TaskAddRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null || request.getProjectId() == null,
                ErrorCode.PARAMS_ERROR, "所属项目不能为空");
        DesignProject project = assertManageableProject(request.getProjectId(), loginUser);
        assertProjectEditable(project);
        DesignTask task = new DesignTask();
        BeanUtil.copyProperties(request, task);
        task.setTaskCode(StrUtil.trim(task.getTaskCode()));
        task.setTaskName(StrUtil.trim(task.getTaskName()));
        task.setRequirement(StrUtil.trim(task.getRequirement()));
        if (StrUtil.isBlank(task.getPriority())) task.setPriority(ProjectPriorityEnum.MEDIUM.getValue());
        task.setStatus(TaskStatusEnum.UNASSIGNED.getValue());
        task.setProgress(0);
        task.setVersion(0);
        task.setCreateBy(loginUser.getId());
        task.setUpdateBy(loginUser.getId());
        validTask(task);
        ThrowUtils.throwIf(!this.save(task), ErrorCode.OPERATION_ERROR, "创建设计任务失败");
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(TaskUpdateRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getVersion() == null,
                ErrorCode.PARAMS_ERROR, "任务 ID 和版本号不能为空");
        DesignTask old = getAccessibleTask(request.getId(), loginUser);
        assertManageableProject(old.getProjectId(), loginUser);
        ThrowUtils.throwIf(List.of(TaskStatusEnum.PENDING_REVIEW.getValue(),
                        TaskStatusEnum.COMPLETED.getValue(), TaskStatusEnum.CANCELLED.getValue())
                        .contains(old.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前状态的任务不能编辑");
        DesignTask effective = new DesignTask();
        BeanUtil.copyProperties(old, effective);
        BeanUtil.copyProperties(request, effective, CopyOptions.create().ignoreNullValue());
        effective.setTaskCode(old.getTaskCode());
        effective.setProjectId(old.getProjectId());
        effective.setStatus(old.getStatus());
        effective.setAssigneeId(old.getAssigneeId());
        effective.setReviewerId(old.getReviewerId());
        effective.setUpdateBy(loginUser.getId());
        validTask(effective);
        ThrowUtils.throwIf(!this.updateById(effective), ErrorCode.OPERATION_ERROR,
                "任务已被其他用户修改，请刷新后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(TaskAssignRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getVersion() == null
                        || request.getAssigneeId() == null || request.getReviewerId() == null,
                ErrorCode.PARAMS_ERROR, "任务、负责人、审核人和版本号不能为空");
        DesignTask task = getAccessibleTask(request.getId(), loginUser);
        DesignProject project = assertManageableProject(task.getProjectId(), loginUser);
        assertProjectEditable(project);
        ThrowUtils.throwIf(!List.of(TaskStatusEnum.UNASSIGNED.getValue(),
                        TaskStatusEnum.PENDING_ACCEPTANCE.getValue()).contains(task.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有待分配或待领取任务可以重新分派");
        ThrowUtils.throwIf(request.getAssigneeId().equals(request.getReviewerId()),
                ErrorCode.PARAMS_ERROR, "任务负责人和审核人不能是同一人");
        assertProjectMember(task.getProjectId(), request.getAssigneeId(),
                List.of(ProjectMemberRoleEnum.DESIGNER.getValue(), ProjectMemberRoleEnum.MANAGER.getValue()), "负责人");
        assertProjectMember(task.getProjectId(), request.getReviewerId(),
                List.of(ProjectMemberRoleEnum.REVIEWER.getValue(), ProjectMemberRoleEnum.MANAGER.getValue()), "审核人");
        DesignTask update = versionedUpdate(task.getId(), request.getVersion(), loginUser.getId());
        update.setAssigneeId(request.getAssigneeId());
        update.setReviewerId(request.getReviewerId());
        update.setStatus(TaskStatusEnum.PENDING_ACCEPTANCE.getValue());
        update.setProgress(0);
        doVersionedUpdate(update);
        notificationService.send(request.getAssigneeId(), "task", "你有新的设计任务",
                "“" + project.getProjectName() + "”中的任务“" + task.getTaskName()
                        + "”已分派给你，请及时领取。",
                "task", task.getId(), "/tasks",
                "task-assignee:" + task.getId() + ":" + request.getVersion()
                        + ":" + request.getAssigneeId());
        notificationService.send(request.getReviewerId(), "review", "你被指定为任务审核人",
                "你将负责审核“" + task.getTaskName() + "”的设计稿。",
                "task", task.getId(), "/reviews",
                "task-reviewer:" + task.getId() + ":" + request.getVersion()
                        + ":" + request.getReviewerId());
    }

    @Override
    public void acceptTask(Long id, Integer version, User loginUser) {
        DesignTask task = getAccessibleTask(id, loginUser);
        assertAssignee(task, loginUser);
        DesignProject project = designProjectService.getAccessibleProject(task.getProjectId(), loginUser);
        ThrowUtils.throwIf(!ProjectStatusEnum.DESIGNING.getValue().equals(project.getStatus()),
                ErrorCode.OPERATION_ERROR, "项目进入设计中后才能领取任务");
        ThrowUtils.throwIf(!TaskStatusEnum.PENDING_ACCEPTANCE.getValue().equals(task.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有待领取任务可以领取");
        DesignTask update = versionedUpdate(id, version, loginUser.getId());
        update.setStatus(TaskStatusEnum.IN_PROGRESS.getValue());
        doVersionedUpdate(update);
    }

    @Override
    public void updateProgress(TaskProgressRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getVersion() == null
                        || request.getProgress() == null || request.getProgress() < 0
                        || request.getProgress() > 100,
                ErrorCode.PARAMS_ERROR, "进度必须在 0 到 100 之间");
        DesignTask task = getAccessibleTask(request.getId(), loginUser);
        assertAssignee(task, loginUser);
        ThrowUtils.throwIf(!List.of(TaskStatusEnum.IN_PROGRESS.getValue(),
                        TaskStatusEnum.REVISION.getValue()).contains(task.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前状态不能更新进度");
        DesignTask update = versionedUpdate(task.getId(), request.getVersion(), loginUser.getId());
        update.setProgress(request.getProgress());
        doVersionedUpdate(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long id, Integer version, String note, User loginUser) {
        DesignTask task = getAccessibleTask(id, loginUser);
        assertAssignee(task, loginUser);
        ThrowUtils.throwIf(!List.of(TaskStatusEnum.IN_PROGRESS.getValue(),
                        TaskStatusEnum.REVISION.getValue()).contains(task.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前状态不能提交审核");
        ThrowUtils.throwIf(task.getReviewerId() == null, ErrorCode.OPERATION_ERROR, "任务尚未配置审核人");
        DesignFile latestFile = designFileMapper.selectOne(new QueryWrapper<DesignFile>()
                .eq("task_id", task.getId())
                .eq("file_type", DesignFileTypeEnum.DESIGN.getValue())
                .orderByDesc("version_no", "id").last("LIMIT 1"));
        ThrowUtils.throwIf(latestFile == null, ErrorCode.OPERATION_ERROR,
                "至少上传一个有效设计稿版本后才能提交审核");
        latestFile.setSubmittedFlag(1);
        ThrowUtils.throwIf(designFileMapper.updateById(latestFile) != 1,
                ErrorCode.OPERATION_ERROR, "锁定设计稿审核版本失败");
        DesignTask update = versionedUpdate(id, version, loginUser.getId());
        update.setStatus(TaskStatusEnum.PENDING_REVIEW.getValue());
        update.setProgress(100);
        update.setSubmittedFileId(latestFile.getId());
        update.setLastSubmitNote(StrUtil.trim(note));
        doVersionedUpdate(update);
        notificationService.send(task.getReviewerId(), "review", "设计稿等待审核",
                "“" + task.getTaskName() + "”已提交设计稿 V" + latestFile.getVersionNo()
                        + "，请及时审核。",
                "task", task.getId(), "/reviews",
                "review-submit:" + task.getId() + ":" + latestFile.getId());
    }

    @Override
    public void cancelTask(Long id, Integer version, String reason, User loginUser) {
        ThrowUtils.throwIf(StrUtil.isBlank(reason), ErrorCode.PARAMS_ERROR, "取消原因不能为空");
        DesignTask task = getAccessibleTask(id, loginUser);
        assertManageableProject(task.getProjectId(), loginUser);
        ThrowUtils.throwIf(List.of(TaskStatusEnum.COMPLETED.getValue(),
                        TaskStatusEnum.CANCELLED.getValue()).contains(task.getStatus()),
                ErrorCode.OPERATION_ERROR, "已完成或已取消任务不能再次取消");
        DesignTask update = versionedUpdate(id, version, loginUser.getId());
        update.setStatus(TaskStatusEnum.CANCELLED.getValue());
        update.setCancelReason(StrUtil.trim(reason));
        doVersionedUpdate(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewTask(Long id, Integer version, boolean approved, String opinion, User loginUser) {
        DesignTask task = getAccessibleTask(id, loginUser);
        ThrowUtils.throwIf(task.getReviewerId() == null || !task.getReviewerId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只有任务指定审核人可以审核");
        ThrowUtils.throwIf(!TaskStatusEnum.PENDING_REVIEW.getValue().equals(task.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有待审核任务可以审核");
        ThrowUtils.throwIf(!approved && StrUtil.isBlank(opinion),
                ErrorCode.PARAMS_ERROR, "退回修改时审核意见不能为空");
        DesignTask update = versionedUpdate(id, version, loginUser.getId());
        if (approved) {
            update.setStatus(TaskStatusEnum.COMPLETED.getValue());
            update.setProgress(100);
            update.setRejectionReason(null);
        } else {
            update.setStatus(TaskStatusEnum.REVISION.getValue());
            update.setRejectionReason(StrUtil.trim(opinion));
        }
        doVersionedUpdate(update);
    }

    @Override
    public DesignTask getAccessibleTask(Long id, User loginUser) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        DesignTask task = this.getById(id);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "设计任务不存在");
        designProjectService.getAccessibleProject(task.getProjectId(), loginUser);
        return task;
    }

    @Override
    public QueryWrapper<DesignTask> getQueryWrapper(TaskQueryRequest request, User loginUser, boolean mine) {
        if (request == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        QueryWrapper<DesignTask> wrapper = new QueryWrapper<>();
        if (mine) {
            wrapper.and(nested -> nested.eq("assignee_id", loginUser.getId())
                    .or().eq("reviewer_id", loginUser.getId()));
        }
        if (!userService.hasRole(loginUser.getId(), "admin")) {
            wrapper.apply("EXISTS (SELECT 1 FROM project_member pm WHERE pm.project_id = "
                    + "design_task.project_id AND pm.user_id = {0} AND pm.is_delete = 0)", loginUser.getId());
        }
        wrapper.and(StrUtil.isNotBlank(request.getKeyword()), nested -> nested
                .like("task_name", request.getKeyword()).or().like("task_code", request.getKeyword()));
        wrapper.eq(request.getProjectId() != null, "project_id", request.getProjectId());
        wrapper.eq(request.getAssigneeId() != null, "assignee_id", request.getAssigneeId());
        wrapper.eq(request.getReviewerId() != null, "reviewer_id", request.getReviewerId());
        wrapper.eq(StrUtil.isNotBlank(request.getPriority()), "priority", request.getPriority());
        wrapper.eq(StrUtil.isNotBlank(request.getStatus()), "status", request.getStatus());
        wrapper.ge(request.getDeadlineFrom() != null, "deadline", request.getDeadlineFrom());
        wrapper.le(request.getDeadlineTo() != null, "deadline", request.getDeadlineTo());
        Map<String, String> sorts = Map.of("taskCode", "task_code", "taskName", "task_name",
                "status", "status", "priority", "priority", "deadline", "deadline",
                "progress", "progress", "createTime", "create_time", "updateTime", "update_time");
        String column = StrUtil.isBlank(request.getSortField()) ? null : sorts.get(request.getSortField());
        if (column == null) wrapper.orderByDesc("id");
        else wrapper.orderBy(true, "ascend".equals(request.getSortOrder()), column);
        return wrapper;
    }

    @Override
    public TaskVO getTaskVO(DesignTask task) {
        if (task == null) return null;
        DesignProject project = designProjectMapper.selectById(task.getProjectId());
        User assignee = task.getAssigneeId() == null ? null : userMapper.selectById(task.getAssigneeId());
        User reviewer = task.getReviewerId() == null ? null : userMapper.selectById(task.getReviewerId());
        return convert(task, project, assignee, reviewer);
    }

    @Override
    public Page<TaskVO> getTaskVOPage(Page<DesignTask> page) {
        Page<TaskVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (page.getRecords().isEmpty()) { result.setRecords(new ArrayList<>()); return result; }
        List<Long> projectIds = page.getRecords().stream().map(DesignTask::getProjectId).distinct().toList();
        Map<Long, DesignProject> projects = designProjectMapper.selectByIds(projectIds).stream()
                .collect(Collectors.toMap(DesignProject::getId, Function.identity()));
        List<Long> userIds = page.getRecords().stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getAssigneeId(), item.getReviewerId()))
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? new HashMap<>() : userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        result.setRecords(page.getRecords().stream().map(task -> convert(task,
                projects.get(task.getProjectId()), users.get(task.getAssigneeId()),
                users.get(task.getReviewerId()))).toList());
        return result;
    }

    private void validTask(DesignTask task) {
        ThrowUtils.throwIf(StrUtil.isBlank(task.getTaskCode())
                        || !task.getTaskCode().matches("^[A-Za-z0-9][A-Za-z0-9_-]{2,49}$"),
                ErrorCode.PARAMS_ERROR, "任务编号格式不合法");
        ThrowUtils.throwIf(StrUtil.isBlank(task.getTaskName()) || task.getTaskName().length() > 120,
                ErrorCode.PARAMS_ERROR, "任务名称不能为空且不能超过 120 个字符");
        ThrowUtils.throwIf(!ProjectPriorityEnum.isValid(task.getPriority()),
                ErrorCode.PARAMS_ERROR, "任务优先级不合法");
        ThrowUtils.throwIf(!TaskStatusEnum.isValid(task.getStatus()),
                ErrorCode.PARAMS_ERROR, "任务状态不合法");
        QueryWrapper<DesignTask> duplicate = new QueryWrapper<DesignTask>()
                .eq("task_code", task.getTaskCode()).ne(task.getId() != null, "id", task.getId());
        ThrowUtils.throwIf(this.baseMapper.selectCount(duplicate) > 0,
                ErrorCode.PARAMS_ERROR, "任务编号已存在");
    }

    private DesignProject assertManageableProject(Long projectId, User loginUser) {
        DesignProject project = designProjectService.getAccessibleProject(projectId, loginUser);
        if (userService.hasRole(loginUser.getId(), "admin") || project.getManagerId().equals(loginUser.getId())) return project;
        ProjectMember member = projectMemberMapper.selectOne(new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId).eq("user_id", loginUser.getId()).last("LIMIT 1"));
        ThrowUtils.throwIf(member == null || !ProjectMemberRoleEnum.MANAGER.getValue().equals(member.getProjectRole()),
                ErrorCode.NO_AUTH_ERROR, "只有项目负责人或项目经理可以维护任务");
        return project;
    }

    private void assertProjectEditable(DesignProject project) {
        ThrowUtils.throwIf(!List.of(ProjectStatusEnum.APPROVED.getValue(),
                        ProjectStatusEnum.DESIGNING.getValue()).contains(project.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有已立项或设计中的项目可以维护任务");
    }

    private void assertProjectMember(Long projectId, Long userId, List<String> roles, String label) {
        ProjectMember member = projectMemberMapper.selectOne(new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId).eq("user_id", userId).last("LIMIT 1"));
        ThrowUtils.throwIf(member == null || !roles.contains(member.getProjectRole()),
                ErrorCode.PARAMS_ERROR, label + "必须是具有对应项目角色的有效成员");
    }

    private void assertAssignee(DesignTask task, User loginUser) {
        ThrowUtils.throwIf(task.getAssigneeId() == null || !task.getAssigneeId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只有任务负责人可以执行此操作");
    }

    private DesignTask versionedUpdate(Long id, Integer version, Long userId) {
        ThrowUtils.throwIf(version == null, ErrorCode.PARAMS_ERROR, "版本号不能为空");
        DesignTask update = new DesignTask();
        update.setId(id);
        update.setVersion(version);
        update.setUpdateBy(userId);
        return update;
    }

    private void doVersionedUpdate(DesignTask update) {
        ThrowUtils.throwIf(!this.updateById(update), ErrorCode.OPERATION_ERROR,
                "任务已被其他用户修改，请刷新后重试");
    }

    private TaskVO convert(DesignTask task, DesignProject project, User assignee, User reviewer) {
        TaskVO vo = new TaskVO();
        BeanUtil.copyProperties(task, vo);
        vo.setId(String.valueOf(task.getId()));
        vo.setProjectId(String.valueOf(task.getProjectId()));
        if (task.getAssigneeId() != null) vo.setAssigneeId(String.valueOf(task.getAssigneeId()));
        if (task.getReviewerId() != null) vo.setReviewerId(String.valueOf(task.getReviewerId()));
        if (task.getSubmittedFileId() != null) vo.setSubmittedFileId(String.valueOf(task.getSubmittedFileId()));
        if (project != null) { vo.setProjectCode(project.getProjectCode()); vo.setProjectName(project.getProjectName()); }
        if (assignee != null) vo.setAssigneeName(assignee.getUserName());
        if (reviewer != null) vo.setReviewerName(reviewer.getUserName());
        return vo;
    }
}
