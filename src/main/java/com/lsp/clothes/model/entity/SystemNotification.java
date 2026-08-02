package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("system_notification")
public class SystemNotification implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long receiverId;
    private String type;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private String route;
    private Integer readFlag;
    private Date readTime;
    private String bizKey;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
