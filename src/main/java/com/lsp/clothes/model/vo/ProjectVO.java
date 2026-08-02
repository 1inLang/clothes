package com.lsp.clothes.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

@Data
public class ProjectVO implements Serializable {
    private String id;
    private String projectCode;
    private String projectName;
    private String category;
    private String season;
    private String style;
    private String targetAudience;
    private String requirement;
    private String managerId;
    private String managerName;
    private String acceptorId;
    private String acceptorName;
    private String priority;
    private String status;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private Integer progress;
    private Integer version;
    private String lastRejectionReason;
    private String cancelReason;
    private Date createTime;
    private Date updateTime;
    private static final long serialVersionUID = 1L;
}
