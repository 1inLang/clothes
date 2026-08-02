package com.lsp.clothes.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.dto.role.RoleQueryRequest;
import com.lsp.clothes.model.entity.SysPermission;
import com.lsp.clothes.model.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IRepository<SysRole> {

    void validRole(SysRole sysRole, boolean add);

    QueryWrapper<SysRole> getQueryWrapper(RoleQueryRequest roleQueryRequest);

    void assignPermissions(Long roleId, List<Long> permissionIds);

    List<SysPermission> listPermissions(Long roleId);
}
