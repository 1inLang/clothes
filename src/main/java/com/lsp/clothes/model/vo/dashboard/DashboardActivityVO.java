package com.lsp.clothes.model.vo.dashboard;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class DashboardActivityVO implements Serializable {
    private String id;
    private String type;
    private String actorName;
    private String action;
    private String subject;
    private Date createTime;
    private String tone;
    private String route;
    private static final long serialVersionUID = 1L;
}
