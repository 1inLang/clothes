package com.lsp.clothes.service.Impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.BusinessException;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.mapper.SysPermissionMapper;
import com.lsp.clothes.model.dto.permission.PermissionQueryRequest;
import com.lsp.clothes.model.entity.SysPermission;
import com.lsp.clothes.service.SysPermissionService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SysPermissionServiceImpl
        extends CrudRepository<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    @Override
    public void validPermission(SysPermission permission, boolean add) {
        ThrowUtils.throwIf(permission == null, ErrorCode.PARAMS_ERROR);
        String permissionName = permission.getPermissionName();
        String permissionCode = permission.getPermissionCode();
        Integer permissionType = permission.getPermissionType();
        Long parentId = permission.getParentId();
        Integer sortOrder = permission.getSortOrder();
        Integer status = permission.getStatus();

        if (add || StrUtil.isNotBlank(permissionName)) {
            ThrowUtils.throwIf(StrUtil.isBlank(permissionName)
                            || permissionName.length() > 100,
                    ErrorCode.PARAMS_ERROR, "权限名称不能为空且不能超过 100 个字符");
        }
        if (add || StrUtil.isNotBlank(permissionCode)) {
            ThrowUtils.throwIf(StrUtil.isBlank(permissionCode)
                            || !permissionCode.matches("^[a-z][a-z0-9:_-]{1,99}$"),
                    ErrorCode.PARAMS_ERROR, "权限编码格式不正确");
        }
        ThrowUtils.throwIf(permissionType != null
                        && permissionType != 1 && permissionType != 2,
                ErrorCode.PARAMS_ERROR, "权限类型只能是 1 或 2");
        ThrowUtils.throwIf(parentId != null && parentId < 0,
                ErrorCode.PARAMS_ERROR, "父权限 ID 不合法");
        ThrowUtils.throwIf(permission.getId() != null
                        && permission.getId().equals(parentId),
                ErrorCode.PARAMS_ERROR, "父权限不能是自身");
        ThrowUtils.throwIf(sortOrder != null && sortOrder < 0,
                ErrorCode.PARAMS_ERROR, "显示顺序不能小于 0");
        ThrowUtils.throwIf(status != null && status != 0 && status != 1,
                ErrorCode.PARAMS_ERROR, "权限状态只能是 0 或 1");

        if (parentId != null && parentId > 0) {
            ThrowUtils.throwIf(this.getById(parentId) == null,
                    ErrorCode.NOT_FOUND_ERROR, "父权限不存在");
        }
        if (StrUtil.isNotBlank(permissionCode)) {
            QueryWrapper<SysPermission> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("permission_code", permissionCode);
            queryWrapper.ne(permission.getId() != null, "id", permission.getId());
            ThrowUtils.throwIf(this.baseMapper.selectCount(queryWrapper) > 0,
                    ErrorCode.PARAMS_ERROR, "权限编码已存在");
        }
    }

    @Override
    public QueryWrapper<SysPermission> getQueryWrapper(PermissionQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<SysPermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(request.getId()), "id", request.getId());
        queryWrapper.like(StrUtil.isNotBlank(request.getPermissionName()),
                "permission_name", request.getPermissionName());
        queryWrapper.like(StrUtil.isNotBlank(request.getPermissionCode()),
                "permission_code", request.getPermissionCode());
        queryWrapper.eq(ObjUtil.isNotNull(request.getPermissionType()),
                "permission_type", request.getPermissionType());
        queryWrapper.eq(ObjUtil.isNotNull(request.getParentId()),
                "parent_id", request.getParentId());
        queryWrapper.eq(ObjUtil.isNotNull(request.getStatus()), "status", request.getStatus());

        Map<String, String> sortFieldMap = Map.of(
                "id", "id",
                "permissionName", "permission_name",
                "permissionCode", "permission_code",
                "permissionType", "permission_type",
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
            queryWrapper.orderByAsc("sort_order").orderByAsc("id");
        }
        return queryWrapper;
    }
}
