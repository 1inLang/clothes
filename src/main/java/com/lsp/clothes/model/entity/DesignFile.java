package com.lsp.clothes.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("design_file")
public class DesignFile implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private Long taskId;
    private String fileName;
    private String storageKey;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private Integer versionNo;
    private String versionNote;
    private Integer submittedFlag;
    private Long uploaderId;
    private Date createTime;
    @TableLogic
    private Integer isDelete;
    private static final long serialVersionUID = 1L;
}
