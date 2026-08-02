package com.lsp.clothes.model.dto.permission;

import com.lsp.clothes.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 权限分页查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PermissionQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String permissionName;

    private String permissionCode;

    private Integer permissionType;

    private Long parentId;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
