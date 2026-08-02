package com.lsp.clothes.service;

import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.dashboard.DashboardActivityVO;
import com.lsp.clothes.model.vo.dashboard.DashboardProjectStatusVO;
import com.lsp.clothes.model.vo.dashboard.DashboardSummaryVO;
import com.lsp.clothes.model.vo.dashboard.DashboardTaskCompletionVO;
import com.lsp.clothes.model.vo.dashboard.DashboardTodoVO;

import java.util.List;

public interface DashboardService {
    DashboardSummaryVO getSummary(User loginUser);
    List<DashboardTodoVO> getMyTodos(User loginUser, int limit);
    DashboardProjectStatusVO getProjectStatus(User loginUser);
    DashboardTaskCompletionVO getTaskCompletion(User loginUser);
    List<DashboardActivityVO> getRecentActivities(User loginUser, int limit);
}
