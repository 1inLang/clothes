package com.lsp.clothes.model.vo.dashboard;

import lombok.Data;

import java.io.Serializable;

@Data
public class DashboardTaskCompletionVO implements Serializable {
    private Long total;
    private Long completed;
    private Long inProgress;
    private Long overdue;
    private Integer completionRate;
    private static final long serialVersionUID = 1L;
}
