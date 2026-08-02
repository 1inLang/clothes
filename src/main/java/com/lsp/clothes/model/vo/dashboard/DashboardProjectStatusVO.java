package com.lsp.clothes.model.vo.dashboard;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DashboardProjectStatusVO implements Serializable {
    private List<DashboardStatusCountVO> distribution;
    private List<DashboardProjectItemVO> projects;
    private static final long serialVersionUID = 1L;
}
