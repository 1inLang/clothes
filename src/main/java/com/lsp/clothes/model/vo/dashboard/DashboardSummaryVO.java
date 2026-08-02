package com.lsp.clothes.model.vo.dashboard;

import lombok.Data;

import java.io.Serializable;

@Data
public class DashboardSummaryVO implements Serializable {
    private Integer myTodoCount;
    private Integer pendingReviewCount;
    private Integer pendingAcceptanceCount;
    private Integer dueSoonCount;
    private Integer overdueCount;
    private static final long serialVersionUID = 1L;
}
