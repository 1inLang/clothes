package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.dto.project.member.ProjectMemberAddRequest;
import com.lsp.clothes.model.dto.project.member.ProjectMemberRemoveRequest;
import com.lsp.clothes.model.dto.project.member.ProjectMemberUpdateRequest;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.ProjectMemberVO;
import com.lsp.clothes.model.vo.UserVO;
import com.lsp.clothes.service.ProjectMemberService;
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
@RequestMapping("/project/member")
public class ProjectMemberController {

    @Resource
    private ProjectMemberService projectMemberService;

    @Resource
    private UserService userService;

    @GetMapping("/list")
    @SaCheckPermission("project:view")
    public BaseResponse<List<ProjectMemberVO>> list(@RequestParam long projectId) {
        ThrowUtils.throwIf(projectId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(projectMemberService.listMembers(projectId, loginUser()));
    }

    @GetMapping("/candidates")
    @SaCheckPermission("project:member")
    public BaseResponse<List<UserVO>> candidates(@RequestParam long projectId,
                                                 @RequestParam(required = false) String keyword) {
        ThrowUtils.throwIf(projectId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(projectMemberService.listCandidates(projectId, keyword, loginUser()));
    }

    @PostMapping("/add")
    @SaCheckPermission("project:member")
    public BaseResponse<Long> add(@RequestBody ProjectMemberAddRequest request) {
        return ResultUtils.success(projectMemberService.addMember(request, loginUser()));
    }

    @PostMapping("/update")
    @SaCheckPermission("project:member")
    public BaseResponse<Boolean> update(@RequestBody ProjectMemberUpdateRequest request) {
        projectMemberService.updateMember(request, loginUser());
        return ResultUtils.success(true);
    }

    @PostMapping("/remove")
    @SaCheckPermission("project:member")
    public BaseResponse<Boolean> remove(@RequestBody ProjectMemberRemoveRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        projectMemberService.removeMember(request.getId(), loginUser());
        return ResultUtils.success(true);
    }

    private User loginUser() {
        return userService.getLoginUser(null);
    }
}
