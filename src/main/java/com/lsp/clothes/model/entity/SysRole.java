package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统角色表
 *
 * @TableName sys_role
 */
@TableName(value = "sys_role")
@Data
public class SysRole implements Serializable {

    /**
     * 角色 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色说明
     */
    private String description;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 状态：0 停用，1 启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除：0 否，1 是
     */
    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
