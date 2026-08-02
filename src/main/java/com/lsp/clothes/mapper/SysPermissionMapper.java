package com.lsp.clothes.mapper;

import com.lsp.clothes.model.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 琳琅
* @description 针对表【sys_permission(系统权限表)】的数据库操作Mapper
* @createDate 2026-07-29 17:58:38
* @Entity com.lsp.clothes.model.entity.SysPermission
*/
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    List<SysPermission> selectByRoleId(Long roleId);

    List<String> selectPermissionCodesByRoleCode(String roleCode);

    List<String> selectPermissionCodesByUserId(Long userId);
}




