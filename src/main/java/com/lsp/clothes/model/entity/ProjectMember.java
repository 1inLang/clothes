package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("project_member")
public class ProjectMember implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long userId;
    private String projectRole;
    private Date joinTime;
    private Long createBy;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
    private static final long serialVersionUID = 1L;
}
