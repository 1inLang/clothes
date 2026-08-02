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
import com.lsp.clothes.model.dto.permission.PermissionAddRequest;
import com.lsp.clothes.model.dto.permission.PermissionQueryRequest;
import com.lsp.clothes.model.dto.permission.PermissionUpdateRequest;
import com.lsp.clothes.model.entity.SysPermission;
import com.lsp.clothes.service.SysPermissionService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 权限管理接口
 */
@RestController
@RequestMapping("/permission")
@SaCheckRole("admin")
public class PermissionController {

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 创建权限
     */
    @PostMapping("/add")
    public BaseResponse<Long> addPermission(@RequestBody PermissionAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        SysPermission permission = new SysPermission();
        BeanUtils.copyProperties(request, permission);
        permission.setPermissionName(StrUtil.trim(permission.getPermissionName()));
        permission.setPermissionCode(StrUtil.trim(permission.getPermissionCode()));
        if (permission.getPermissionType() == null) {
            permission.setPermissionType(2);
        }
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getSortOrder() == null) {
            permission.setSortOrder(0);
        }
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }
        sysPermissionService.validPermission(permission, true);
        ThrowUtils.throwIf(!sysPermissionService.save(permission),
                ErrorCode.OPERATION_ERROR, "创建权限失败");
        return ResultUtils.success(permission.getId());
    }

    /**
     * 更新权限
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePermission(
            @RequestBody PermissionUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(sysPermissionService.getById(request.getId()) == null,
                ErrorCode.NOT_FOUND_ERROR, "权限不存在");
        SysPermission permission = new SysPermission();
        BeanUtils.copyProperties(request, permission);
        permission.setPermissionName(StrUtil.trim(permission.getPermissionName()));
        permission.setPermissionCode(StrUtil.trim(permission.getPermissionCode()));
        sysPermissionService.validPermission(permission, false);
        ThrowUtils.throwIf(!sysPermissionService.updateById(permission),
                ErrorCode.OPERATION_ERROR, "更新权限失败");
        return ResultUtils.success(true);
    }

    /**
     * 删除权限
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePermission(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        SysPermission permission = sysPermissionService.getById(request.getId());
        ThrowUtils.throwIf(permission == null,
                ErrorCode.NOT_FOUND_ERROR, "权限不存在或已删除");

        QueryWrapper<SysPermission> childQuery = new QueryWrapper<>();
        childQuery.eq("parent_id", permission.getId());
        ThrowUtils.throwIf(sysPermissionService.count(childQuery) > 0,
                ErrorCode.OPERATION_ERROR, "该权限仍有子权限，不能删除");
        ThrowUtils.throwIf(!sysPermissionService.removeById(permission.getId()),
                ErrorCode.OPERATION_ERROR, "删除权限失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据 ID 查询权限
     */
    @GetMapping("/get")
    public BaseResponse<SysPermission> getPermissionById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        SysPermission permission = sysPermissionService.getById(id);
        ThrowUtils.throwIf(permission == null, ErrorCode.NOT_FOUND_ERROR, "权限不存在");
        return ResultUtils.success(permission);
    }

    /**
     * 分页查询权限
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<SysPermission>> listPermissionByPage(
            @RequestBody PermissionQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Page<SysPermission> permissionPage = sysPermissionService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                sysPermissionService.getQueryWrapper(request)
        );
        return ResultUtils.success(permissionPage);
    }
}
