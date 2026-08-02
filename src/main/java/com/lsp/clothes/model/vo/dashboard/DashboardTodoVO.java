package com.lsp.clothes.model.vo.dashboard;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DashboardTodoVO implements Serializable {
    private String id;
    private String businessType;
    private String title;
    private String subtitle;
    private String status;
    private String priority;
    private LocalDateTime deadline;
    private Boolean overdue;
    private String route;
    private static final long serialVersionUID = 1L;
}
