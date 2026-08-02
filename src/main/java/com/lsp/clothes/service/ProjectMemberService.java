package com.lsp.clothes.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.dto.project.member.ProjectMemberAddRequest;
import com.lsp.clothes.model.dto.project.member.ProjectMemberUpdateRequest;
import com.lsp.clothes.model.entity.ProjectMember;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.ProjectMemberVO;
import com.lsp.clothes.model.vo.UserVO;

import java.util.List;

public interface ProjectMemberService extends IRepository<ProjectMember> {
    List<ProjectMemberVO> listMembers(Long projectId, User loginUser);
    List<UserVO> listCandidates(Long projectId, String keyword, User loginUser);
    Long addMember(ProjectMemberAddRequest request, User loginUser);
    void updateMember(ProjectMemberUpdateRequest request, User loginUser);
    void removeMember(Long memberId, User loginUser);
    void ensureManagerMember(Long projectId, Long managerId, Long operatorId);
}
