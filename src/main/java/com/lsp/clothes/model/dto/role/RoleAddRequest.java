package com.lsp.clothes.model.dto.role;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色创建请求
 */
@Data
public class RoleAddRequest implements Serializable {

    private String roleName;

    private String description;

    private String roleCode;

    private Integer sortOrder;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
