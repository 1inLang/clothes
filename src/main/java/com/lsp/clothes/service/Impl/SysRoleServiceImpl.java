package com.lsp.clothes.service.Impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.BusinessException;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.mapper.SysPermissionMapper;
import com.lsp.clothes.mapper.SysRoleMapper;
import com.lsp.clothes.mapper.SysRolePermissionMapper;
import com.lsp.clothes.model.dto.role.RoleQueryRequest;
import com.lsp.clothes.model.entity.SysPermission;
import com.lsp.clothes.model.entity.SysRole;
import com.lsp.clothes.model.entity.SysRolePermission;
import com.lsp.clothes.service.SysRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SysRoleServiceImpl extends CrudRepository<SysRoleMapper, SysRole>
        implements SysRoleService {

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public void validRole(SysRole sysRole, boolean add) {
        ThrowUtils.throwIf(sysRole == null, ErrorCode.PARAMS_ERROR);
        String roleName = sysRole.getRoleName();
        String roleCode = sysRole.getRoleCode();
        Integer sortOrder = sysRole.getSortOrder();
        Integer status = sysRole.getStatus();

        if (add || StrUtil.isNotBlank(roleName)) {
            ThrowUtils.throwIf(StrUtil.isBlank(roleName) || roleName.length() > 50,
                    ErrorCode.PARAMS_ERROR, "角色名称不能为空且不能超过 50 个字符");
        }
        if (add || StrUtil.isNotBlank(roleCode)) {
            ThrowUtils.throwIf(StrUtil.isBlank(roleCode)
                            || !roleCode.matches("^[a-z][a-z0-9_]{1,49}$"),
                    ErrorCode.PARAMS_ERROR, "角色编码只能使用小写字母、数字和下划线");
        }
        ThrowUtils.throwIf(sortOrder != null && sortOrder < 0,
                ErrorCode.PARAMS_ERROR, "显示顺序不能小于 0");
        ThrowUtils.throwIf(status != null && status != 0 && status != 1,
                ErrorCode.PARAMS_ERROR, "角色状态只能是 0 或 1");

        if (StrUtil.isNotBlank(roleCode)) {
            QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("role_code", roleCode);
            queryWrapper.ne(sysRole.getId() != null, "id", sysRole.getId());
            ThrowUtils.throwIf(this.baseMapper.selectCount(queryWrapper) > 0,
                    ErrorCode.PARAMS_ERROR, "角色编码已存在");
        }
    }

    @Override
    public QueryWrapper<SysRole> getQueryWrapper(RoleQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(request.getId()), "id", request.getId());
        queryWrapper.like(StrUtil.isNotBlank(request.getRoleName()),
                "role_name", request.getRoleName());
        queryWrapper.eq(StrUtil.isNotBlank(request.getRoleCode()),
                "role_code", request.getRoleCode());
        queryWrapper.like(StrUtil.isNotBlank(request.getDescription()),
                "description", request.getDescription());
        queryWrapper.eq(ObjUtil.isNotNull(request.getStatus()), "status", request.getStatus());

        Map<String, String> sortFieldMap = Map.of(
                "id", "id",
                "roleName", "role_name",
                "roleCode", "role_code",
                "sortOrder", "sort_order",
                "status", "status",
                "createTime", "create_time",
                "updateTime", "update_time"
        );
        String sortColumn = StrUtil.isNotBlank(request.getSortField())
                ? sortFieldMap.get(request.getSortField()) : null;
        if (sortColumn != null) {
            queryWrapper.orderBy(true, "ascend".equals(request.getSortOrder()), sortColumn);
        } else {
            queryWrapper.orderByAsc("sort_order").orderByDesc("id");
        }
        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        ThrowUtils.throwIf(roleId == null || roleId <= 0,
                ErrorCode.PARAMS_ERROR, "角色 ID 不合法");
        ThrowUtils.throwIf(this.getById(roleId) == null,
                ErrorCode.NOT_FOUND_ERROR, "角色不存在");

        Set<Long> distinctIds = permissionIds == null
                ? Collections.emptySet() : new LinkedHashSet<>(permissionIds);
        ThrowUtils.throwIf(distinctIds.stream().anyMatch(id -> id == null || id <= 0),
                ErrorCode.PARAMS_ERROR, "权限 ID 不合法");

        if (!distinctIds.isEmpty()) {
            QueryWrapper<SysPermission> permissionQuery = new QueryWrapper<>();
            permissionQuery.in("id", distinctIds);
            Long count = sysPermissionMapper.selectCount(permissionQuery);
            ThrowUtils.throwIf(count != distinctIds.size(),
                    ErrorCode.NOT_FOUND_ERROR, "部分权限不存在或已删除");
        }

        QueryWrapper<SysRolePermission> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("role_id", roleId);
        sysRolePermissionMapper.delete(deleteWrapper);

        for (Long permissionId : distinctIds) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            ThrowUtils.throwIf(sysRolePermissionMapper.insert(relation) != 1,
                    ErrorCode.OPERATION_ERROR, "角色授权失败");
        }
    }

    @Override
    public List<SysPermission> listPermissions(Long roleId) {
        ThrowUtils.throwIf(roleId == null || roleId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(this.getById(roleId) == null,
                ErrorCode.NOT_FOUND_ERROR, "角色不存在");
        return sysPermissionMapper.selectByRoleId(roleId);
    }
}
