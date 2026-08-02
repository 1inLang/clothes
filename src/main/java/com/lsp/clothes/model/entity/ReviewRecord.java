package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("review_record")
public class ReviewRecord implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;
    private Long fileId;
    private Integer versionNo;
    private Long reviewerId;
    private String result;
    private String opinion;
    private String requestNo;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
