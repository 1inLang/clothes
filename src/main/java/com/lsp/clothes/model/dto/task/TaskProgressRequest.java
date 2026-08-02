package com.lsp.clothes.model.dto.task;

import lombok.Data;
import java.io.Serializable;

@Data
public class TaskProgressRequest implements Serializable {
    private Long id;
    private Integer version;
    private Integer progress;
    private static final long serialVersionUID = 1L;
}
