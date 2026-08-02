package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 角色权限关联表
 *
 * @TableName sys_role_permission
 */
@TableName(value = "sys_role_permission")
@Data
public class SysRolePermission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long permissionId;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
