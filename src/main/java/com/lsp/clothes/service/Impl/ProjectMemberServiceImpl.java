package com.lsp.clothes.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.mapper.ProjectMemberMapper;
import com.lsp.clothes.mapper.UserMapper;
import com.lsp.clothes.model.dto.project.member.ProjectMemberAddRequest;
import com.lsp.clothes.model.dto.project.member.ProjectMemberUpdateRequest;
import com.lsp.clothes.model.entity.DesignProject;
import com.lsp.clothes.model.entity.ProjectMember;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.enums.ProjectMemberRoleEnum;
import com.lsp.clothes.model.enums.ProjectStatusEnum;
import com.lsp.clothes.model.vo.ProjectMemberVO;
import com.lsp.clothes.model.vo.UserVO;
import com.lsp.clothes.service.DesignProjectService;
import com.lsp.clothes.service.NotificationService;
import com.lsp.clothes.service.ProjectMemberService;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProjectMemberServiceImpl
        extends CrudRepository<ProjectMemberMapper, ProjectMember>
        implements ProjectMemberService {

    @Resource
    private DesignProjectService designProjectService;

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private NotificationService notificationService;

    @Override
    public List<ProjectMemberVO> listMembers(Long projectId, User loginUser) {
        designProjectService.getAccessibleProject(projectId, loginUser);
        List<ProjectMember> members = this.list(new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId).orderByAsc("join_time"));
        if (members.isEmpty()) return new ArrayList<>();
        Map<Long, User> users = userMapper.selectByIds(members.stream()
                        .map(ProjectMember::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream().map(member -> toVO(member, users.get(member.getUserId()))).toList();
    }

    @Override
    public List<UserVO> listCandidates(Long projectId, String keyword, User loginUser) {
        assertManageable(projectId, loginUser);
        List<Long> memberIds = this.list(new QueryWrapper<ProjectMember>()
                        .select("user_id").eq("project_id", projectId)).stream()
                .map(ProjectMember::getUserId).toList();
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_status", 1)
                .notIn(!memberIds.isEmpty(), "id", memberIds)
                .and(StrUtil.isNotBlank(keyword), nested -> nested
                        .like("user_name", keyword).or().like("user_account", keyword))
                .orderByAsc("user_name").last("LIMIT 50");
        return userService.getUserVOList(userMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addMember(ProjectMemberAddRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null || request.getProjectId() == null
                        || request.getUserId() == null,
                ErrorCode.PARAMS_ERROR, "项目和用户不能为空");
        DesignProject project = assertManageable(request.getProjectId(), loginUser);
        assertEditable(project);
        validateRoleAndUser(request.getProjectRole(), request.getUserId());
        ThrowUtils.throwIf(findMember(request.getProjectId(), request.getUserId()) != null,
                ErrorCode.OPERATION_ERROR, "该用户已经是项目成员");
        ProjectMember member = new ProjectMember();
        member.setProjectId(request.getProjectId());
        member.setUserId(request.getUserId());
        member.setProjectRole(request.getProjectRole());
        member.setCreateBy(loginUser.getId());
        ThrowUtils.throwIf(!this.save(member), ErrorCode.OPERATION_ERROR, "添加项目成员失败");
        notificationService.send(member.getUserId(), "project", "你已加入项目",
                "你已被加入“" + project.getProjectName() + "”，项目内角色为“"
                        + ProjectMemberRoleEnum.getTextByValue(member.getProjectRole()) + "”。",
                "project", project.getId(), "/projects", "member-add:" + member.getId());
        return member.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMember(ProjectMemberUpdateRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null || request.getId() == null,
                ErrorCode.PARAMS_ERROR, "成员关系 ID 不能为空");
        ProjectMember member = this.getById(request.getId());
        ThrowUtils.throwIf(member == null, ErrorCode.NOT_FOUND_ERROR, "项目成员不存在");
        DesignProject project = assertManageable(member.getProjectId(), loginUser);
        assertEditable(project);
        validateRoleAndUser(request.getProjectRole(), member.getUserId());
        ThrowUtils.throwIf(project.getManagerId().equals(member.getUserId())
                        && !ProjectMemberRoleEnum.MANAGER.getValue().equals(request.getProjectRole()),
                ErrorCode.OPERATION_ERROR, "项目负责人必须保留项目经理角色");
        ProjectMember update = new ProjectMember();
        update.setId(member.getId());
        update.setProjectRole(request.getProjectRole());
        ThrowUtils.throwIf(!this.updateById(update), ErrorCode.OPERATION_ERROR, "更新成员角色失败");
        notificationService.send(member.getUserId(), "project", "项目角色已调整",
                "你在“" + project.getProjectName() + "”中的角色已调整为“"
                        + ProjectMemberRoleEnum.getTextByValue(request.getProjectRole()) + "”。",
                "project", project.getId(), "/projects",
                "member-role:" + member.getId() + ":" + request.getProjectRole()
                        + ":" + System.currentTimeMillis());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long memberId, User loginUser) {
        ThrowUtils.throwIf(memberId == null || memberId <= 0, ErrorCode.PARAMS_ERROR);
        ProjectMember member = this.getById(memberId);
        ThrowUtils.throwIf(member == null, ErrorCode.NOT_FOUND_ERROR, "项目成员不存在");
        DesignProject project = assertManageable(member.getProjectId(), loginUser);
        assertEditable(project);
        ThrowUtils.throwIf(project.getManagerId().equals(member.getUserId()),
                ErrorCode.OPERATION_ERROR, "不能移除当前项目负责人，请先变更负责人");
        ThrowUtils.throwIf(!this.removeById(memberId), ErrorCode.OPERATION_ERROR, "移除项目成员失败");
        notificationService.send(member.getUserId(), "project", "你已被移出项目",
                "你已被移出“" + project.getProjectName() + "”。",
                "project", project.getId(), "/projects", "member-remove:" + member.getId());
    }

    @Override
    public void ensureManagerMember(Long projectId, Long managerId, Long operatorId) {
        ProjectMember member = findMember(projectId, managerId);
        if (member == null) {
            member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(managerId);
            member.setProjectRole(ProjectMemberRoleEnum.MANAGER.getValue());
            member.setCreateBy(operatorId);
            ThrowUtils.throwIf(!this.save(member), ErrorCode.OPERATION_ERROR, "同步项目负责人失败");
        } else if (!ProjectMemberRoleEnum.MANAGER.getValue().equals(member.getProjectRole())) {
            member.setProjectRole(ProjectMemberRoleEnum.MANAGER.getValue());
            ThrowUtils.throwIf(!this.updateById(member), ErrorCode.OPERATION_ERROR, "同步项目负责人失败");
        }
    }

    private DesignProject assertManageable(Long projectId, User loginUser) {
        DesignProject project = designProjectService.getAccessibleProject(projectId, loginUser);
        if (userService.hasRole(loginUser.getId(), "admin")
                || project.getManagerId().equals(loginUser.getId())) {
            return project;
        }
        ProjectMember member = findMember(projectId, loginUser.getId());
        ThrowUtils.throwIf(member == null
                        || !ProjectMemberRoleEnum.MANAGER.getValue().equals(member.getProjectRole()),
                ErrorCode.NO_AUTH_ERROR, "只有项目负责人或项目经理可以维护成员");
        return project;
    }

    private void assertEditable(DesignProject project) {
        ThrowUtils.throwIf(ProjectStatusEnum.COMPLETED.getValue().equals(project.getStatus())
                        || ProjectStatusEnum.CANCELLED.getValue().equals(project.getStatus()),
                ErrorCode.OPERATION_ERROR, "已完成或已取消项目不能修改成员");
    }

    private void validateRoleAndUser(String projectRole, Long userId) {
        ThrowUtils.throwIf(!ProjectMemberRoleEnum.isValid(projectRole),
                ErrorCode.PARAMS_ERROR, "项目内角色不合法");
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null || user.getUserStatus() == null || user.getUserStatus() != 1,
                ErrorCode.PARAMS_ERROR, "用户不存在或已停用");
        if (ProjectMemberRoleEnum.MANAGER.getValue().equals(projectRole)) {
            ThrowUtils.throwIf(!userService.hasRole(userId, "project_manager")
                            && !userService.hasRole(userId, "admin"),
                    ErrorCode.PARAMS_ERROR, "项目经理角色只能分配给项目经理或管理员账号");
        }
    }

    private ProjectMember findMember(Long projectId, Long userId) {
        return this.getOne(new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId).eq("user_id", userId).last("LIMIT 1"));
    }

    private ProjectMemberVO toVO(ProjectMember member, User user) {
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setId(String.valueOf(member.getId()));
        vo.setProjectId(String.valueOf(member.getProjectId()));
        vo.setUserId(String.valueOf(member.getUserId()));
        vo.setProjectRole(member.getProjectRole());
        vo.setJoinTime(member.getJoinTime());
        if (user != null) {
            vo.setUserAccount(user.getUserAccount());
            vo.setUserName(user.getUserName());
            vo.setUserAvatar(user.getUserAvatar());
            UserVO userVO = userService.getUserVO(user);
            vo.setUserRole(userVO.getUserRole());
            vo.setUserRoles(userVO.getUserRoles());
        }
        return vo;
    }
}
