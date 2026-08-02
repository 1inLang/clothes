package com.lsp.clothes.model.dto.role;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色分配权限请求
 */
@Data
public class RoleAssignPermissionsRequest implements Serializable {

    private Long roleId;

    private List<Long> permissionIds;

    private static final long serialVersionUID = 1L;
}
