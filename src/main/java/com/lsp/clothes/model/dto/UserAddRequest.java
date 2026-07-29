package com.lsp.clothes.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建请求
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: USER("普通用户", "user"),
     *     DESIGNER("设计师", "designer"),
     *     REVIEWER("审核人", "reviewer"),
     *     PROJECT_MANAGER("项目经理", "project_manager"),
     *     ADMIN("管理员", "admin");
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}