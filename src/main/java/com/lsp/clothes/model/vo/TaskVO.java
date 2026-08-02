package com.lsp.clothes.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;

@Data
public class TaskVO implements Serializable {
    private String id;
    private String projectId;
    private String projectCode;
    private String projectName;
    private String taskCode;
    private String taskName;
    private String requirement;
    private String assigneeId;
    private String assigneeName;
    private String reviewerId;
    private String reviewerName;
    private String priority;
    private String status;
    private LocalDateTime deadline;
    private Integer progress;
    private Integer version;
    private String submittedFileId;
    private String lastSubmitNote;
    private String rejectionReason;
    private String cancelReason;
    private Date createTime;
    private Date updateTime;
    private static final long serialVersionUID = 1L;
}
