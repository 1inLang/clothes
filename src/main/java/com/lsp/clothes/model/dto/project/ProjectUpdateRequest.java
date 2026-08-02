package com.lsp.clothes.model.dto.project;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class ProjectUpdateRequest implements Serializable {
    private Long id;
    private Integer version;
    private String projectName;
    private String category;
    private String season;
    private String style;
    private String targetAudience;
    private String requirement;
    private Long managerId;
    private Long acceptorId;
    private String priority;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private Integer progress;
    private static final long serialVersionUID = 1L;
}
