package com.lsp.clothes.model.dto.task;

import lombok.Data;
import java.io.Serializable;

@Data
public class TaskAssignRequest implements Serializable {
    private Long id;
    private Integer version;
    private Long assigneeId;
    private Long reviewerId;
    private static final long serialVersionUID = 1L;
}
