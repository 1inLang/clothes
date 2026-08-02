package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

@TableName("design_project")
@Data
public class DesignProject implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String projectCode;
    private String projectName;
    private String category;
    private String season;
    private String style;
    private String targetAudience;
    private String requirement;
    private Long managerId;
    private Long acceptorId;
    private String priority;
    private String status;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private Integer progress;
    @Version
    private Integer version;
    private String lastRejectionReason;
    private String cancelReason;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
    private static final long serialVersionUID = 1L;
}
