package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.DeleteRequest;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.dto.role.RoleAddRequest;
import com.lsp.clothes.model.dto.role.RoleAssignPermissionsRequest;
import com.lsp.clothes.model.dto.role.RoleQueryRequest;
import com.lsp.clothes.model.dto.role.RoleUpdateRequest;
import com.lsp.clothes.model.entity.SysPermission;
import com.lsp.clothes.model.entity.SysRole;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.entity.UserRole;
import com.lsp.clothes.mapper.UserRoleMapper;
import com.lsp.clothes.service.SysRoleService;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/role")
@SaCheckRole("admin")
public class RoleController {

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private UserService userService;

    @Resource
    private UserRoleMapper userRoleMapper;

    /**
     * 创建角色
     */
    @PostMapping("/add")
    public BaseResponse<Long> addRole(@RequestBody RoleAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        role.setRoleName(StrUtil.trim(role.getRoleName()));
        role.setRoleCode(StrUtil.trim(role.getRoleCode()));
        if (role.getSortOrder() == null) {
            role.setSortOrder(0);
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        sysRoleService.validRole(role, true);
        ThrowUtils.throwIf(!sysRoleService.save(role),
                ErrorCode.OPERATION_ERROR, "创建角色失败");
        return ResultUtils.success(role.getId());
    }

    /**
     * 更新角色
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateRole(@RequestBody RoleUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        SysRole oldRole = sysRoleService.getById(request.getId());
        ThrowUtils.throwIf(oldRole == null, ErrorCode.NOT_FOUND_ERROR, "角色不存在");

        if (StrUtil.isNotBlank(request.getRoleCode())
                && !oldRole.getRoleCode().equals(request.getRoleCode())) {
            ThrowUtils.throwIf(countUsersByRoleCode(oldRole.getRoleCode()) > 0,
                    ErrorCode.OPERATION_ERROR, "该角色已分配给用户，不能修改角色编码");
        }

        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        role.setRoleName(StrUtil.trim(role.getRoleName()));
        role.setRoleCode(StrUtil.trim(role.getRoleCode()));
        sysRoleService.validRole(role, false);
        ThrowUtils.throwIf(!sysRoleService.updateById(role),
                ErrorCode.OPERATION_ERROR, "更新角色失败");
        return ResultUtils.success(true);
    }

    /**
     * 删除角色
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteRole(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        SysRole role = sysRoleService.getById(request.getId());
        ThrowUtils.throwIf(role == null, ErrorCode.NOT_FOUND_ERROR, "角色不存在或已删除");
        ThrowUtils.throwIf(countUsersByRoleCode(role.getRoleCode()) > 0,
                ErrorCode.OPERATION_ERROR, "该角色已分配给用户，不能删除");
        ThrowUtils.throwIf(!sysRoleService.removeById(role.getId()),
                ErrorCode.OPERATION_ERROR, "删除角色失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据 ID 查询角色
     */
    @GetMapping("/get")
    public BaseResponse<SysRole> getRoleById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        SysRole role = sysRoleService.getById(id);
        ThrowUtils.throwIf(role == null, ErrorCode.NOT_FOUND_ERROR, "角色不存在");
        return ResultUtils.success(role);
    }

    /**
     * 分页查询角色
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<SysRole>> listRoleByPage(
            @RequestBody RoleQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Page<SysRole> rolePage = sysRoleService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                sysRoleService.getQueryWrapper(request)
        );
        return ResultUtils.success(rolePage);
    }

    /**
     * 为角色重新分配权限
     */
    @PostMapping("/assign/permissions")
    public BaseResponse<Boolean> assignPermissions(
            @RequestBody RoleAssignPermissionsRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        sysRoleService.assignPermissions(request.getRoleId(), request.getPermissionIds());
        return ResultUtils.success(true);
    }

    /**
     * 查询角色已拥有的权限
     */
    @GetMapping("/list/permissions")
    public BaseResponse<List<SysPermission>> listPermissions(long roleId) {
        return ResultUtils.success(sysRoleService.listPermissions(roleId));
    }

    private long countUsersByRoleCode(String roleCode) {
        return userRoleMapper.countActiveUsersByRoleCode(roleCode);
    }
}
