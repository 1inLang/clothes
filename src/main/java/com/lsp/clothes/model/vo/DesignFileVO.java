package com.lsp.clothes.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class DesignFileVO implements Serializable {
    private String id;
    private String projectId;
    private String taskId;
    private String fileName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private Integer versionNo;
    private String versionNote;
    private String uploaderId;
    private String uploaderName;
    private Date createTime;
    private Boolean submitted;
    private static final long serialVersionUID = 1L;
}
