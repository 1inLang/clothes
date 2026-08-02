package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;

@Data
@TableName("design_task")
public class DesignTask implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private String taskCode;
    private String taskName;
    private String requirement;
    private Long assigneeId;
    private Long reviewerId;
    private String priority;
    private String status;
    private LocalDateTime deadline;
    private Integer progress;
    @Version
    private Integer version;
    private Long submittedFileId;
    private String lastSubmitNote;
    private String rejectionReason;
    private String cancelReason;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
    private static final long serialVersionUID = 1L;
}
