package com.lsp.clothes.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.dto.permission.PermissionQueryRequest;
import com.lsp.clothes.model.entity.SysPermission;

public interface SysPermissionService extends IRepository<SysPermission> {

    void validPermission(SysPermission sysPermission, boolean add);

    QueryWrapper<SysPermission> getQueryWrapper(PermissionQueryRequest permissionQueryRequest);
}
