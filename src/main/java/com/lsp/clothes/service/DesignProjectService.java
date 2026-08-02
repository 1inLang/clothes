package com.lsp.clothes.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.dto.project.ProjectQueryRequest;
import com.lsp.clothes.model.dto.project.ProjectUpdateRequest;
import com.lsp.clothes.model.entity.DesignProject;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.ProjectAcceptanceRecordVO;
import com.lsp.clothes.model.vo.ProjectVO;
import com.lsp.clothes.model.vo.UserVO;

import java.util.List;

public interface DesignProjectService extends IRepository<DesignProject> {
    Long createProject(DesignProject project);
    void validProject(DesignProject project, boolean add);
    QueryWrapper<DesignProject> getQueryWrapper(ProjectQueryRequest request, User loginUser);
    ProjectVO getProjectVO(DesignProject project);
    Page<ProjectVO> getProjectVOPage(Page<DesignProject> projectPage);
    DesignProject getAccessibleProject(Long projectId, User loginUser);
    void updateProject(ProjectUpdateRequest request, User loginUser);
    void transition(Long projectId, Integer version, String expectedStatus,
                    String targetStatus, String reason, User loginUser);
    void reviewAcceptance(Long projectId, Integer version, boolean approved,
                          String opinion, String requestNo, User loginUser);
    List<ProjectAcceptanceRecordVO> listAcceptanceHistory(Long projectId, User loginUser);
    List<UserVO> listAcceptorCandidates(String keyword);
}
