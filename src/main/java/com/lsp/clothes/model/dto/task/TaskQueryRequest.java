package com.lsp.clothes.model.dto.task;

import com.lsp.clothes.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQueryRequest extends PageRequest implements Serializable {
    private String keyword;
    private Long projectId;
    private Long assigneeId;
    private Long reviewerId;
    private String priority;
    private String status;
    private LocalDateTime deadlineFrom;
    private LocalDateTime deadlineTo;
    private static final long serialVersionUID = 1L;
}
