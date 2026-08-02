package com.lsp.clothes.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.dto.task.TaskAddRequest;
import com.lsp.clothes.model.dto.task.TaskAssignRequest;
import com.lsp.clothes.model.dto.task.TaskProgressRequest;
import com.lsp.clothes.model.dto.task.TaskQueryRequest;
import com.lsp.clothes.model.dto.task.TaskUpdateRequest;
import com.lsp.clothes.model.entity.DesignTask;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.TaskVO;

public interface DesignTaskService extends IRepository<DesignTask> {
    Long addTask(TaskAddRequest request, User loginUser);
    void updateTask(TaskUpdateRequest request, User loginUser);
    void assignTask(TaskAssignRequest request, User loginUser);
    void acceptTask(Long id, Integer version, User loginUser);
    void updateProgress(TaskProgressRequest request, User loginUser);
    void submitReview(Long id, Integer version, String note, User loginUser);
    void cancelTask(Long id, Integer version, String reason, User loginUser);
    void reviewTask(Long id, Integer version, boolean approved, String opinion, User loginUser);
    DesignTask getAccessibleTask(Long id, User loginUser);
    QueryWrapper<DesignTask> getQueryWrapper(TaskQueryRequest request, User loginUser, boolean mine);
    TaskVO getTaskVO(DesignTask task);
    Page<TaskVO> getTaskVOPage(Page<DesignTask> page);
}
