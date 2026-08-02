package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.dashboard.DashboardActivityVO;
import com.lsp.clothes.model.vo.dashboard.DashboardProjectStatusVO;
import com.lsp.clothes.model.vo.dashboard.DashboardSummaryVO;
import com.lsp.clothes.model.vo.dashboard.DashboardTaskCompletionVO;
import com.lsp.clothes.model.vo.dashboard.DashboardTodoVO;
import com.lsp.clothes.service.DashboardService;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@SaCheckPermission("dashboard:view")
public class DashboardController {
    @Resource
    private DashboardService dashboardService;
    @Resource
    private UserService userService;

    @GetMapping("/summary")
    public BaseResponse<DashboardSummaryVO> summary() {
        return ResultUtils.success(dashboardService.getSummary(loginUser()));
    }

    @GetMapping("/my-todos")
    public BaseResponse<List<DashboardTodoVO>> myTodos(
            @RequestParam(defaultValue = "8") int limit) {
        validateLimit(limit);
        return ResultUtils.success(dashboardService.getMyTodos(loginUser(), limit));
    }

    @GetMapping("/project-status")
    public BaseResponse<DashboardProjectStatusVO> projectStatus() {
        return ResultUtils.success(dashboardService.getProjectStatus(loginUser()));
    }

    @GetMapping("/task-completion")
    public BaseResponse<DashboardTaskCompletionVO> taskCompletion() {
        return ResultUtils.success(dashboardService.getTaskCompletion(loginUser()));
    }

    @GetMapping("/recent-activities")
    public BaseResponse<List<DashboardActivityVO>> recentActivities(
            @RequestParam(defaultValue = "8") int limit) {
        validateLimit(limit);
        return ResultUtils.success(dashboardService.getRecentActivities(loginUser(), limit));
    }

    private User loginUser() {
        return userService.getLoginUser(null);
    }

    private void validateLimit(int limit) {
        ThrowUtils.throwIf(limit <= 0 || limit > 20, ErrorCode.PARAMS_ERROR,
                "返回数量必须在 1 到 20 之间");
    }
}
