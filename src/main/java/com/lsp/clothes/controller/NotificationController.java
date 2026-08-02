package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.dto.notification.NotificationQueryRequest;
import com.lsp.clothes.model.dto.notification.NotificationReadRequest;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.NotificationVO;
import com.lsp.clothes.service.NotificationService;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
public class NotificationController {
    @Resource
    private NotificationService notificationService;
    @Resource
    private UserService userService;

    @PostMapping("/list/page")
    @SaCheckPermission("notification:view")
    public BaseResponse<Page<NotificationVO>> page(
            @RequestBody NotificationQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getCurrent() <= 0
                        || request.getPageSize() <= 0 || request.getPageSize() > 100,
                ErrorCode.PARAMS_ERROR, "分页参数不合法");
        return ResultUtils.success(notificationService.pageNotifications(request, loginUser()));
    }

    @GetMapping("/unread-count")
    @SaCheckPermission("notification:view")
    public BaseResponse<Long> unreadCount() {
        return ResultUtils.success(notificationService.unreadCount(loginUser()));
    }

    @PostMapping("/read")
    @SaCheckPermission("notification:update")
    public BaseResponse<Boolean> read(@RequestBody NotificationReadRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "通知 ID 不能为空");
        notificationService.markRead(request.getId(), loginUser());
        return ResultUtils.success(true);
    }

    @PostMapping("/read-all")
    @SaCheckPermission("notification:update")
    public BaseResponse<Boolean> readAll() {
        notificationService.markAllRead(loginUser());
        return ResultUtils.success(true);
    }

    private User loginUser() {
        return userService.getLoginUser(null);
    }
}
