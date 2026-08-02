package com.lsp.clothes.model.dto.project;

import com.lsp.clothes.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectQueryRequest extends PageRequest implements Serializable {
    private String keyword;
    private Long id;
    private String projectCode;
    private String projectName;
    private String category;
    private String season;
    private String priority;
    private String status;
    private Long managerId;
    private LocalDate planStartFrom;
    private LocalDate planEndTo;
    private static final long serialVersionUID = 1L;
}
