package com.lsp.clothes.model.dto.user;

import com.lsp.clothes.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：USER("普通用户", "user"),
     *     DESIGNER("设计师", "designer"),
     *     REVIEWER("审核人", "reviewer"),
     *     PROJECT_ACCEPTOR("项目验收人", "project_acceptor"),
     *     PROJECT_MANAGER("项目经理", "project_manager"),
     *     ADMIN("管理员", "admin");
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
