package com.lsp.clothes.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LoginUserVO implements Serializable {
    /**
     * id
     */
    private String id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    private List<String> userRoles;

    private static final long serialVersionUID = 1L;
}
