package com.lsp.clothes.model.dto.permission;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限创建请求
 */
@Data
public class PermissionAddRequest implements Serializable {

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
