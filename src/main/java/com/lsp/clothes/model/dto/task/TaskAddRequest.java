package com.lsp.clothes.model.dto.task;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TaskAddRequest implements Serializable {
    private Long projectId;
    private String taskCode;
    private String taskName;
    private String requirement;
    private String priority;
    private LocalDateTime deadline;
    private static final long serialVersionUID = 1L;
}
