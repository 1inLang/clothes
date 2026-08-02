package com.lsp.clothes.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.mapper.DesignTaskMapper;
import com.lsp.clothes.mapper.SystemNotificationMapper;
import com.lsp.clothes.model.dto.notification.NotificationQueryRequest;
import com.lsp.clothes.model.entity.DesignTask;
import com.lsp.clothes.model.entity.SystemNotification;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.enums.TaskStatusEnum;
import com.lsp.clothes.model.vo.NotificationVO;
import com.lsp.clothes.service.NotificationService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class NotificationServiceImpl
        extends CrudRepository<SystemNotificationMapper, SystemNotification>
        implements NotificationService {
    private static final Set<String> TYPES = Set.of("task", "review", "acceptance", "project", "deadline");

    @Resource
    private DesignTaskMapper designTaskMapper;
    @Override
    public void send(Long receiverId, String type, String title, String content,
                     String businessType, Long businessId, String route, String bizKey) {
        if (receiverId == null || receiverId <= 0 || StrUtil.isBlank(bizKey)) return;
        SystemNotification notification = new SystemNotification();
        notification.setReceiverId(receiverId);
        notification.setType(TYPES.contains(type) ? type : "project");
        notification.setTitle(StrUtil.sub(StrUtil.blankToDefault(title, "业务提醒"), 0, 100));
        notification.setContent(StrUtil.sub(StrUtil.blankToDefault(content, "你有一条新的业务消息"), 0, 500));
        notification.setBusinessType(StrUtil.sub(businessType, 0, 30));
        notification.setBusinessId(businessId);
        notification.setRoute(StrUtil.sub(route, 0, 200));
        notification.setReadFlag(0);
        notification.setBizKey(StrUtil.sub(bizKey, 0, 128));
        try {
            this.save(notification);
        } catch (DuplicateKeyException ignored) {
            // 同一业务事件只保留一条通知。
        }
    }

    @Override
    public Page<NotificationVO> pageNotifications(NotificationQueryRequest request, User loginUser) {
        generateDeadlineReminders(loginUser);
        QueryWrapper<SystemNotification> wrapper = new QueryWrapper<SystemNotification>()
                .eq("receiver_id", loginUser.getId())
                .eq(StrUtil.isNotBlank(request.getType()), "type", request.getType())
                .eq(Boolean.TRUE.equals(request.getUnreadOnly()), "read_flag", 0)
                .orderByAsc("read_flag").orderByDesc("create_time", "id");
        Page<SystemNotification> page = this.page(
                new Page<>(request.getCurrent(), request.getPageSize()), wrapper);
        Page<NotificationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public long unreadCount(User loginUser) {
        generateDeadlineReminders(loginUser);
        Long count = this.baseMapper.selectCount(new QueryWrapper<SystemNotification>()
                .eq("receiver_id", loginUser.getId()).eq("read_flag", 0));
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id, User loginUser) {
        SystemNotification notification = this.getById(id);
        ThrowUtils.throwIf(notification == null
                        || !notification.getReceiverId().equals(loginUser.getId()),
                ErrorCode.NOT_FOUND_ERROR, "通知不存在");
        if (notification.getReadFlag() != null && notification.getReadFlag() == 1) return;
        int rows = this.baseMapper.update(null, new UpdateWrapper<SystemNotification>()
                .eq("id", id).eq("receiver_id", loginUser.getId()).eq("read_flag", 0)
                .set("read_flag", 1).set("read_time", new Date()));
        ThrowUtils.throwIf(rows != 1, ErrorCode.OPERATION_ERROR, "通知状态更新失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(User loginUser) {
        this.baseMapper.update(null, new UpdateWrapper<SystemNotification>()
                .eq("receiver_id", loginUser.getId()).eq("read_flag", 0)
                .set("read_flag", 1).set("read_time", new Date()));
    }

    @Override
    public void generateDeadlineReminders(User loginUser) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime boundary = now.plusDays(3);
        List<DesignTask> tasks = designTaskMapper.selectList(new QueryWrapper<DesignTask>()
                .eq("assignee_id", loginUser.getId())
                .notIn("status", List.of(TaskStatusEnum.COMPLETED.getValue(),
                        TaskStatusEnum.CANCELLED.getValue()))
                .ge("deadline", now).le("deadline", boundary));
        if (tasks.isEmpty()) return;
        for (DesignTask task : tasks) {
            long hours = Math.max(1, Duration.between(now, task.getDeadline()).toHours());
            long days = Math.max(1, (hours + 23) / 24);
            send(loginUser.getId(), "deadline", "任务即将到期",
                    "“" + task.getTaskName() + "”将在 " + days + " 天内到期，请及时处理。",
                    "task", task.getId(), "/tasks",
                    "deadline:" + task.getId() + ":" + task.getDeadline());
        }
    }

    private NotificationVO toVO(SystemNotification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(String.valueOf(notification.getId()));
        vo.setType(notification.getType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setBusinessType(notification.getBusinessType());
        if (notification.getBusinessId() != null) {
            vo.setBusinessId(String.valueOf(notification.getBusinessId()));
        }
        vo.setRoute(notification.getRoute());
        vo.setRead(notification.getReadFlag() != null && notification.getReadFlag() == 1);
        vo.setReadTime(notification.getReadTime());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }
}
