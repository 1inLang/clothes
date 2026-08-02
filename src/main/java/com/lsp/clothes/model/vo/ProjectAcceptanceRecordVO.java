package com.lsp.clothes.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ProjectAcceptanceRecordVO implements Serializable {
    private String id;
    private String projectId;
    private Integer projectVersion;
    private String acceptorId;
    private String acceptorName;
    private String result;
    private String opinion;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
