package com.lsp.clothes.model.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class ProjectProgressVO implements Serializable {
    private String id;
    private String status;
    private Integer progress;
    private Integer version;
    private static final long serialVersionUID = 1L;
}
