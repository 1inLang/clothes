package com.lsp.clothes.model.vo.dashboard;

import lombok.Data;

import java.io.Serializable;

@Data
public class DashboardProjectItemVO implements Serializable {
    private String id;
    private String projectCode;
    private String projectName;
    private String category;
    private String status;
    private Integer progress;
    private Long totalTasks;
    private Long completedTasks;
    private static final long serialVersionUID = 1L;
}
