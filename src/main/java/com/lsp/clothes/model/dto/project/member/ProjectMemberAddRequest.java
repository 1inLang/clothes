package com.lsp.clothes.model.dto.project.member;

import lombok.Data;
import java.io.Serializable;

@Data
public class ProjectMemberAddRequest implements Serializable {
    private Long projectId;
    private Long userId;
    private String projectRole;
    private static final long serialVersionUID = 1L;
}
