package com.lsp.clothes.model.dto.role;

import com.lsp.clothes.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 角色分页查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
