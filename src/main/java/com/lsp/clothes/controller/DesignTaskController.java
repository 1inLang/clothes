package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.dto.task.TaskActionRequest;
import com.lsp.clothes.model.dto.task.TaskAddRequest;
import com.lsp.clothes.model.dto.task.TaskAssignRequest;
import com.lsp.clothes.model.dto.task.TaskProgressRequest;
import com.lsp.clothes.model.dto.task.TaskQueryRequest;
import com.lsp.clothes.model.dto.task.TaskUpdateRequest;
import com.lsp.clothes.model.entity.DesignTask;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.TaskVO;
import com.lsp.clothes.service.DesignTaskService;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class DesignTaskController {

    @Resource
    private DesignTaskService designTaskService;
    @Resource
    private UserService userService;

    @PostMapping("/list/page")
    @SaCheckPermission("task:view")
    public BaseResponse<Page<TaskVO>> listPage(@RequestBody TaskQueryRequest request) {
        return page(request, false);
    }

    @PostMapping("/my/page")
    @SaCheckPermission("task:view")
    public BaseResponse<Page<TaskVO>> myPage(@RequestBody TaskQueryRequest request) {
        return page(request, true);
    }

    @GetMapping("/get")
    @SaCheckPermission("task:view")
    public BaseResponse<TaskVO> get(@RequestParam long id) {
        return ResultUtils.success(designTaskService.getTaskVO(
                designTaskService.getAccessibleTask(id, loginUser())));
    }

    @PostMapping("/add")
    @SaCheckPermission("task:create")
    public BaseResponse<Long> add(@RequestBody TaskAddRequest request) {
        return ResultUtils.success(designTaskService.addTask(request, loginUser()));
    }

    @PostMapping("/update")
    @SaCheckPermission("task:create")
    public BaseResponse<Boolean> update(@RequestBody TaskUpdateRequest request) {
        designTaskService.updateTask(request, loginUser());
        return ResultUtils.success(true);
    }

    @PostMapping("/assign")
    @SaCheckPermission("task:assign")
    public BaseResponse<Boolean> assign(@RequestBody TaskAssignRequest request) {
        designTaskService.assignTask(request, loginUser());
        return ResultUtils.success(true);
    }

    @PostMapping("/accept")
    @SaCheckPermission("task:submit")
    public BaseResponse<Boolean> accept(@RequestBody TaskActionRequest request) {
        validateAction(request);
        designTaskService.acceptTask(request.getId(), request.getVersion(), loginUser());
        return ResultUtils.success(true);
    }

    @PostMapping("/update-progress")
    @SaCheckPermission("task:submit")
    public BaseResponse<Boolean> updateProgress(@RequestBody TaskProgressRequest request) {
        designTaskService.updateProgress(request, loginUser());
        return ResultUtils.success(true);
    }

    @PostMapping("/submit-review")
    @SaCheckPermission("task:submit")
    public BaseResponse<Boolean> submitReview(@RequestBody TaskActionRequest request) {
        validateAction(request);
        designTaskService.submitReview(request.getId(), request.getVersion(), request.getReason(), loginUser());
        return ResultUtils.success(true);
    }

    @PostMapping("/cancel")
    @SaCheckPermission("task:assign")
    public BaseResponse<Boolean> cancel(@RequestBody TaskActionRequest request) {
        validateAction(request);
        designTaskService.cancelTask(request.getId(), request.getVersion(), request.getReason(), loginUser());
        return ResultUtils.success(true);
    }

    private BaseResponse<Page<TaskVO>> page(TaskQueryRequest request, boolean mine) {
        ThrowUtils.throwIf(request == null || request.getCurrent() <= 0 || request.getPageSize() <= 0
                        || request.getPageSize() > 100,
                ErrorCode.PARAMS_ERROR, "分页参数不合法");
        User loginUser = loginUser();
        Page<DesignTask> result = designTaskService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                designTaskService.getQueryWrapper(request, loginUser, mine));
        return ResultUtils.success(designTaskService.getTaskVOPage(result));
    }

    private void validateAction(TaskActionRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0
                        || request.getVersion() == null,
                ErrorCode.PARAMS_ERROR, "任务 ID 和版本号不能为空");
    }

    private User loginUser() {
        return userService.getLoginUser(null);
    }
}
