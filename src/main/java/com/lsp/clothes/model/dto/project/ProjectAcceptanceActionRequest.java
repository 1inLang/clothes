package com.lsp.clothes.model.dto.project;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProjectAcceptanceActionRequest implements Serializable {
    private Long id;
    private Integer version;
    private String opinion;
    private String requestNo;
    private static final long serialVersionUID = 1L;
}
