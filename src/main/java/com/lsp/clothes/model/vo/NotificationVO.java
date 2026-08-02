package com.lsp.clothes.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class NotificationVO implements Serializable {
    private String id;
    private String type;
    private String title;
    private String content;
    private String businessType;
    private String businessId;
    private String route;
    private Boolean read;
    private Date readTime;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
