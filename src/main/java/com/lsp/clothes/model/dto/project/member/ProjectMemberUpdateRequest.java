package com.lsp.clothes.model.dto.project.member;

import lombok.Data;
import java.io.Serializable;

@Data
public class ProjectMemberUpdateRequest implements Serializable {
    private Long id;
    private String projectRole;
    private static final long serialVersionUID = 1L;
}
