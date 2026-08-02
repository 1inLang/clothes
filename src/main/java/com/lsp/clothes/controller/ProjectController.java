package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.dto.project.ProjectActionRequest;
import com.lsp.clothes.model.dto.project.ProjectAcceptanceActionRequest;
import com.lsp.clothes.model.dto.project.ProjectAddRequest;
import com.lsp.clothes.model.dto.project.ProjectQueryRequest;
import com.lsp.clothes.model.dto.project.ProjectUpdateRequest;
import com.lsp.clothes.model.entity.DesignProject;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.enums.ProjectPriorityEnum;
import com.lsp.clothes.model.enums.ProjectStatusEnum;
import com.lsp.clothes.model.vo.ProjectProgressVO;
import com.lsp.clothes.model.vo.ProjectAcceptanceRecordVO;
import com.lsp.clothes.model.vo.ProjectVO;
import com.lsp.clothes.model.vo.UserVO;
import com.lsp.clothes.service.DesignProjectService;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设计项目管理接口。
 */
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Resource
    private DesignProjectService designProjectService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    @SaCheckPermission("project:create")
    public BaseResponse<Long> addProject(@RequestBody ProjectAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(null);
        DesignProject project = new DesignProject();
        BeanUtils.copyProperties(request, project);
        trimProject(project);
        if (project.getManagerId() == null) {
            project.setManagerId(loginUser.getId());
        }
        if (StrUtil.isBlank(project.getPriority())) {
            project.setPriority(ProjectPriorityEnum.MEDIUM.getValue());
        }
        project.setStatus(ProjectStatusEnum.DRAFT.getValue());
        project.setProgress(0);
        project.setVersion(0);
        project.setCreateBy(loginUser.getId());
        project.setUpdateBy(loginUser.getId());
        designProjectService.validProject(project, true);
        return ResultUtils.success(designProjectService.createProject(project));
    }

    @PostMapping("/update")
    @SaCheckPermission("project:update")
    public BaseResponse<Boolean> updateProject(@RequestBody ProjectUpdateRequest request) {
        designProjectService.updateProject(request, userService.getLoginUser(null));
        return ResultUtils.success(true);
    }

    @PostMapping("/list/page")
    @SaCheckPermission("project:view")
    public BaseResponse<Page<ProjectVO>> listProjectByPage(
            @RequestBody ProjectQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getCurrent() <= 0 || request.getPageSize() <= 0
                        || request.getPageSize() > 100,
                ErrorCode.PARAMS_ERROR, "分页参数不合法");
        User loginUser = userService.getLoginUser(null);
        Page<DesignProject> page = designProjectService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                designProjectService.getQueryWrapper(request, loginUser));
        return ResultUtils.success(designProjectService.getProjectVOPage(page));
    }

    @GetMapping("/get")
    @SaCheckPermission("project:view")
    public BaseResponse<ProjectVO> getProject(@RequestParam long id) {
        DesignProject project = designProjectService.getAccessibleProject(
                id, userService.getLoginUser(null));
        return ResultUtils.success(designProjectService.getProjectVO(project));
    }

    @GetMapping("/progress")
    @SaCheckPermission("project:view")
    public BaseResponse<ProjectProgressVO> getProgress(@RequestParam long id) {
        DesignProject project = designProjectService.getAccessibleProject(
                id, userService.getLoginUser(null));
        ProjectProgressVO progress = new ProjectProgressVO();
        progress.setId(String.valueOf(project.getId()));
        progress.setStatus(project.getStatus());
        progress.setProgress(project.getProgress());
        progress.setVersion(project.getVersion());
        return ResultUtils.success(progress);
    }

    @PostMapping("/submit")
    @SaCheckPermission("project:update")
    public BaseResponse<Boolean> submit(@RequestBody ProjectActionRequest request) {
        transition(request, ProjectStatusEnum.DRAFT, ProjectStatusEnum.APPROVED);
        return ResultUtils.success(true);
    }

    @PostMapping("/start-design")
    @SaCheckPermission("project:update")
    public BaseResponse<Boolean> startDesign(@RequestBody ProjectActionRequest request) {
        transition(request, ProjectStatusEnum.APPROVED, ProjectStatusEnum.DESIGNING);
        return ResultUtils.success(true);
    }

    @PostMapping("/submit-acceptance")
    @SaCheckPermission("project:submit_acceptance")
    public BaseResponse<Boolean> submitAcceptance(@RequestBody ProjectActionRequest request) {
        transition(request, ProjectStatusEnum.DESIGNING, ProjectStatusEnum.ACCEPTANCE);
        return ResultUtils.success(true);
    }

    @PostMapping("/accept")
    @SaCheckPermission("project:acceptance:review")
    public BaseResponse<Boolean> accept(@RequestBody ProjectAcceptanceActionRequest request) {
        reviewAcceptance(request, true);
        return ResultUtils.success(true);
    }

    @PostMapping("/reject-acceptance")
    @SaCheckPermission("project:acceptance:review")
    public BaseResponse<Boolean> rejectAcceptance(
            @RequestBody ProjectAcceptanceActionRequest request) {
        reviewAcceptance(request, false);
        return ResultUtils.success(true);
    }

    @GetMapping("/acceptance/history")
    @SaCheckPermission("project:view")
    public BaseResponse<List<ProjectAcceptanceRecordVO>> acceptanceHistory(
            @RequestParam long id) {
        return ResultUtils.success(designProjectService.listAcceptanceHistory(id,
                userService.getLoginUser(null)));
    }

    @GetMapping("/acceptance/candidates")
    @SaCheckPermission("project:update")
    public BaseResponse<List<UserVO>> acceptanceCandidates(
            @RequestParam(required = false) String keyword) {
        return ResultUtils.success(designProjectService.listAcceptorCandidates(keyword));
    }

    @PostMapping("/cancel")
    @SaCheckPermission("project:close")
    public BaseResponse<Boolean> cancel(@RequestBody ProjectActionRequest request) {
        validateAction(request);
        User loginUser = userService.getLoginUser(null);
        DesignProject project = designProjectService.getAccessibleProject(request.getId(), loginUser);
        ThrowUtils.throwIf(ProjectStatusEnum.COMPLETED.getValue().equals(project.getStatus())
                        || ProjectStatusEnum.CANCELLED.getValue().equals(project.getStatus()),
                ErrorCode.OPERATION_ERROR, "已完成或已取消的项目不能再次取消");
        designProjectService.transition(request.getId(), request.getVersion(),
                project.getStatus(), ProjectStatusEnum.CANCELLED.getValue(),
                request.getReason(), loginUser);
        return ResultUtils.success(true);
    }

    private void transition(ProjectActionRequest request, ProjectStatusEnum expected,
                            ProjectStatusEnum target) {
        validateAction(request);
        designProjectService.transition(request.getId(), request.getVersion(),
                expected.getValue(), target.getValue(), request.getReason(),
                userService.getLoginUser(null));
    }

    private void validateAction(ProjectActionRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null
                        || request.getId() <= 0 || request.getVersion() == null,
                ErrorCode.PARAMS_ERROR, "项目 ID 和版本号不能为空");
    }

    private void reviewAcceptance(ProjectAcceptanceActionRequest request, boolean approved) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        designProjectService.reviewAcceptance(request.getId(), request.getVersion(), approved,
                request.getOpinion(), request.getRequestNo(), userService.getLoginUser(null));
    }

    private void trimProject(DesignProject project) {
        project.setProjectCode(StrUtil.trim(project.getProjectCode()));
        project.setProjectName(StrUtil.trim(project.getProjectName()));
        project.setCategory(StrUtil.trim(project.getCategory()));
        project.setSeason(StrUtil.trim(project.getSeason()));
        project.setStyle(StrUtil.trim(project.getStyle()));
        project.setTargetAudience(StrUtil.trim(project.getTargetAudience()));
        project.setRequirement(StrUtil.trim(project.getRequirement()));
    }
}
