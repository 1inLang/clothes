package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统权限表
 *
 * @TableName sys_permission
 */
@TableName(value = "sys_permission")
@Data
public class SysPermission implements Serializable {

    /**
     * 权限 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * Sa-Token 权限编码
     */
    private String permissionCode;

    /**
     * 类型：1 菜单，2 按钮/接口
     */
    private Integer permissionType;

    /**
     * 父权限 ID，0 表示顶级
     */
    private Long parentId;

    /**
     * 前端路由或接口路径
     */
    private String path;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 状态：0 停用，1 启用
     */
    private Integer status;

    /**
     * 权限说明
     */
    private String description;

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
