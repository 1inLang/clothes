package com.lsp.clothes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsp.clothes.model.entity.UserRole;

import java.util.List;

public interface UserRoleMapper extends BaseMapper<UserRole> {
    List<String> selectRoleCodesByUserId(Long userId);
    long countActiveUsersByRoleCode(String roleCode);
}
