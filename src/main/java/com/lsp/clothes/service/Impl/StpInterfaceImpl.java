package com.lsp.clothes.service.Impl;

import cn.dev33.satoken.stp.StpInterface;
import com.lsp.clothes.mapper.UserMapper;
import com.lsp.clothes.model.entity.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        return user == null ? List.of() : List.of(user.getUserRole());
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return List.of();
        }

        return switch (user.getUserRole()) {
            case "designer" ->
                    List.of("design:create", "design:update");
            case "reviewer" ->
                    List.of("design:review");
            case "project_manager" ->
                    List.of("project:manage", "design:view");
            case "admin" ->
                    List.of(
                            "design:create",
                            "design:update",
                            "design:review",
                            "design:view",
                            "project:manage",
                            "user:manage"
                    );
            default ->
                    List.of("design:view");
        };
    }
}