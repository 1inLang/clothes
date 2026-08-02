package com.lsp.clothes.model.dto.role;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色更新请求
 */
@Data
public class RoleUpdateRequest implements Serializable {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer sortOrder;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
