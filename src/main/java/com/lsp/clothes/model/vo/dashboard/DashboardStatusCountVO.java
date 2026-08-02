package com.lsp.clothes.model.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatusCountVO implements Serializable {
    private String status;
    private Long count;
    private static final long serialVersionUID = 1L;
}
