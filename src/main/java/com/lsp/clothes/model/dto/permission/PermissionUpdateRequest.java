package com.lsp.clothes.model.dto.permission;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限更新请求
 */
@Data
public class PermissionUpdateRequest implements Serializable {

    private Long id;

    private String permissionName;

    private String permissionCode;

    private Integer permissionType;

    private Long parentId;

    private String path;

    private Integer sortOrder;

    private Integer status;

    private String description;

    private static final long serialVersionUID = 1L;
}
