package com.lsp.clothes.model.dto.project.member;

import lombok.Data;
import java.io.Serializable;

@Data
public class ProjectMemberRemoveRequest implements Serializable {
    private Long id;
    private static final long serialVersionUID = 1L;
}
