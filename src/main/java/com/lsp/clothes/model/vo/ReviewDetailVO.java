package com.lsp.clothes.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ReviewDetailVO implements Serializable {
    private TaskVO task;
    private DesignFileVO submittedFile;
    private List<ReviewRecordVO> history;
    private static final long serialVersionUID = 1L;
}
