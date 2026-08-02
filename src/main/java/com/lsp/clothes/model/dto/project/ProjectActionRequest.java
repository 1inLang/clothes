package com.lsp.clothes.model.dto.project;

import lombok.Data;
import java.io.Serializable;

@Data
public class ProjectActionRequest implements Serializable {
    private Long id;
    private Integer version;
    private String reason;
    private static final long serialVersionUID = 1L;
}
