package com.lsp.clothes.service.Impl;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsp.clothes.mapper.SysPermissionMapper;
import com.lsp.clothes.mapper.SysRoleMapper;
import com.lsp.clothes.mapper.UserMapper;
import com.lsp.clothes.mapper.UserRoleMapper;
import com.lsp.clothes.model.entity.SysRole;
import com.lsp.clothes.model.entity.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 角色与权限数据源。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserMapper userMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = getEnabledUser(loginId);
        if (user == null) {
            return List.of();
        }

        return userRoleMapper.selectRoleCodesByUserId(user.getId());
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        User user = getEnabledUser(loginId);
        if (user == null) {
            return List.of();
        }
        return sysPermissionMapper.selectPermissionCodesByUserId(user.getId());
    }

    private User getEnabledUser(Object loginId) {
        if (loginId == null) {
            return null;
        }
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null || user.getUserStatus() == null || user.getUserStatus() != 1) {
            return null;
        }
        return user;
    }
}
