package com.lsp.clothes.model.dto.review;

import lombok.Data;
import java.io.Serializable;

@Data
public class ReviewActionRequest implements Serializable {
    private Long taskId;
    private Integer taskVersion;
    private Integer versionNo;
    private String opinion;
    private String requestNo;
    private static final long serialVersionUID = 1L;
}
