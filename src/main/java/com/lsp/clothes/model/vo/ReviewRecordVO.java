package com.lsp.clothes.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class ReviewRecordVO implements Serializable {
    private String id;
    private String taskId;
    private String fileId;
    private Integer versionNo;
    private String reviewerId;
    private String reviewerName;
    private String result;
    private String opinion;
    private String requestNo;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
