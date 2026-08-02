package com.lsp.clothes.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.dto.notification.NotificationQueryRequest;
import com.lsp.clothes.model.entity.SystemNotification;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.NotificationVO;

public interface NotificationService extends IRepository<SystemNotification> {
    void send(Long receiverId, String type, String title, String content,
              String businessType, Long businessId, String route, String bizKey);
    Page<NotificationVO> pageNotifications(NotificationQueryRequest request, User loginUser);
    long unreadCount(User loginUser);
    void markRead(Long id, User loginUser);
    void markAllRead(User loginUser);
    void generateDeadlineReminders(User loginUser);
}
