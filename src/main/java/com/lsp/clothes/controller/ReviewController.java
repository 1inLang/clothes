package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.dto.review.ReviewActionRequest;
import com.lsp.clothes.model.dto.task.TaskQueryRequest;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.ReviewDetailVO;
import com.lsp.clothes.model.vo.ReviewRecordVO;
import com.lsp.clothes.model.vo.TaskVO;
import com.lsp.clothes.service.ReviewService;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/review")
public class ReviewController {

    @Resource private ReviewService reviewService;
    @Resource private UserService userService;

    @PostMapping("/pending/page")
    @SaCheckPermission("task:review")
    public BaseResponse<Page<TaskVO>> pendingPage(@RequestBody TaskQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getCurrent() <= 0 || request.getPageSize() <= 0
                        || request.getPageSize() > 100,
                ErrorCode.PARAMS_ERROR, "分页参数不合法");
        return ResultUtils.success(reviewService.pendingPage(request, loginUser()));
    }

    @GetMapping("/detail")
    @SaCheckPermission("task:review")
    public BaseResponse<ReviewDetailVO> detail(@RequestParam long taskId) {
        return ResultUtils.success(reviewService.getDetail(taskId, loginUser()));
    }

    @GetMapping("/history")
    @SaCheckPermission("task:view")
    public BaseResponse<List<ReviewRecordVO>> history(@RequestParam long taskId) {
        return ResultUtils.success(reviewService.getHistory(taskId, loginUser()));
    }

    @PostMapping("/approve")
    @SaCheckPermission("task:review")
    public BaseResponse<Boolean> approve(@RequestBody ReviewActionRequest request) {
        reviewService.approve(request, loginUser());
        return ResultUtils.success(true);
    }

    @PostMapping("/reject")
    @SaCheckPermission("task:review")
    public BaseResponse<Boolean> reject(@RequestBody ReviewActionRequest request) {
        reviewService.reject(request, loginUser());
        return ResultUtils.success(true);
    }

    private User loginUser() { return userService.getLoginUser(null); }
}
