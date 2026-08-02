package com.lsp.clothes.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.BusinessException;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.mapper.DesignProjectMapper;
import com.lsp.clothes.mapper.DesignTaskMapper;
import com.lsp.clothes.mapper.ProjectAcceptanceRecordMapper;
import com.lsp.clothes.mapper.ProjectMemberMapper;
import com.lsp.clothes.mapper.UserMapper;
import com.lsp.clothes.mapper.UserRoleMapper;
import com.lsp.clothes.model.dto.project.ProjectQueryRequest;
import com.lsp.clothes.model.dto.project.ProjectUpdateRequest;
import com.lsp.clothes.model.entity.DesignProject;
import com.lsp.clothes.model.entity.DesignTask;
import com.lsp.clothes.model.entity.ProjectAcceptanceRecord;
import com.lsp.clothes.model.entity.ProjectMember;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.enums.ProjectMemberRoleEnum;
import com.lsp.clothes.model.enums.ProjectAcceptanceResultEnum;
import com.lsp.clothes.model.enums.ProjectPriorityEnum;
import com.lsp.clothes.model.enums.ProjectStatusEnum;
import com.lsp.clothes.model.enums.TaskStatusEnum;
import com.lsp.clothes.model.vo.ProjectAcceptanceRecordVO;
import com.lsp.clothes.model.vo.ProjectVO;
import com.lsp.clothes.model.vo.UserVO;
import com.lsp.clothes.service.DesignProjectService;
import com.lsp.clothes.service.NotificationService;
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
public class DesignProjectServiceImpl
        extends CrudRepository<DesignProjectMapper, DesignProject>
        implements DesignProjectService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private DesignTaskMapper designTaskMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private ProjectAcceptanceRecordMapper projectAcceptanceRecordMapper;

    @Resource
    private UserService userService;

    @Resource
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(DesignProject project) {
        ThrowUtils.throwIf(!this.save(project), ErrorCode.OPERATION_ERROR, "创建项目失败");
        ensureManagerMember(project.getId(), project.getManagerId(), project.getCreateBy());
        return project.getId();
    }

    @Override
    public void validProject(DesignProject project, boolean add) {
        ThrowUtils.throwIf(project == null, ErrorCode.PARAMS_ERROR);
        if (add || StrUtil.isNotBlank(project.getProjectCode())) {
            ThrowUtils.throwIf(StrUtil.isBlank(project.getProjectCode())
                            || !project.getProjectCode().matches("^[A-Za-z0-9][A-Za-z0-9_-]{2,49}$"),
                    ErrorCode.PARAMS_ERROR, "项目编号只能使用字母、数字、下划线和短横线");
        }
        if (add || StrUtil.isNotBlank(project.getProjectName())) {
            ThrowUtils.throwIf(StrUtil.isBlank(project.getProjectName())
                            || project.getProjectName().length() > 100,
                    ErrorCode.PARAMS_ERROR, "项目名称不能为空且不能超过 100 个字符");
        }
        ThrowUtils.throwIf(StrUtil.isNotBlank(project.getCategory())
                        && project.getCategory().length() > 50,
                ErrorCode.PARAMS_ERROR, "项目品类不能超过 50 个字符");
        ThrowUtils.throwIf(!ProjectPriorityEnum.isValid(project.getPriority()),
                ErrorCode.PARAMS_ERROR, "项目优先级不合法");
        ThrowUtils.throwIf(!ProjectStatusEnum.isValid(project.getStatus()),
                ErrorCode.PARAMS_ERROR, "项目状态不合法");
        ThrowUtils.throwIf(project.getProgress() == null
                        || project.getProgress() < 0 || project.getProgress() > 100,
                ErrorCode.PARAMS_ERROR, "项目进度必须在 0 到 100 之间");
        ThrowUtils.throwIf(project.getPlanStartDate() != null
                        && project.getPlanEndDate() != null
                        && project.getPlanStartDate().isAfter(project.getPlanEndDate()),
                ErrorCode.PARAMS_ERROR, "计划开始日期不能晚于计划结束日期");
        validateManager(project.getManagerId());
        if (project.getAcceptorId() != null) {
            validateAcceptor(project.getAcceptorId());
            ThrowUtils.throwIf(project.getAcceptorId().equals(project.getManagerId()),
                    ErrorCode.PARAMS_ERROR, "项目负责人和项目验收人不能是同一人");
        }

        QueryWrapper<DesignProject> duplicate = new QueryWrapper<>();
        duplicate.eq("project_code", project.getProjectCode())
                .ne(project.getId() != null, "id", project.getId());
        ThrowUtils.throwIf(this.baseMapper.selectCount(duplicate) > 0,
                ErrorCode.PARAMS_ERROR, "项目编号已存在");
    }

    private void validateManager(Long managerId) {
        ThrowUtils.throwIf(managerId == null || managerId <= 0,
                ErrorCode.PARAMS_ERROR, "项目负责人不能为空");
        User manager = userMapper.selectById(managerId);
        ThrowUtils.throwIf(manager == null || manager.getUserStatus() == null
                        || manager.getUserStatus() != 1,
                ErrorCode.PARAMS_ERROR, "项目负责人不存在或已停用");
        List<String> managerRoles = userRoleMapper.selectRoleCodesByUserId(managerId);
        ThrowUtils.throwIf(!managerRoles.contains("project_manager")
                        && !managerRoles.contains("admin"),
                ErrorCode.PARAMS_ERROR, "项目负责人必须具有项目经理或管理员角色");
    }

    private void validateAcceptor(Long acceptorId) {
        ThrowUtils.throwIf(acceptorId == null || acceptorId <= 0,
                ErrorCode.PARAMS_ERROR, "项目验收人不能为空");
        User acceptor = userMapper.selectById(acceptorId);
        ThrowUtils.throwIf(acceptor == null || acceptor.getUserStatus() == null
                        || acceptor.getUserStatus() != 1,
                ErrorCode.PARAMS_ERROR, "项目验收人不存在或已停用");
        ThrowUtils.throwIf(!userRoleMapper.selectRoleCodesByUserId(acceptorId)
                        .contains("project_acceptor"),
                ErrorCode.PARAMS_ERROR, "指定用户必须具有项目验收人角色");
    }

    @Override
    public QueryWrapper<DesignProject> getQueryWrapper(ProjectQueryRequest request,
                                                       User loginUser) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<DesignProject> wrapper = new QueryWrapper<>();
        if (!isAdmin(loginUser)) {
            wrapper.and(nested -> nested.eq("manager_id", loginUser.getId())
                    .or().eq("acceptor_id", loginUser.getId())
                    .or().apply("EXISTS (SELECT 1 FROM project_member pm "
                            + "WHERE pm.project_id = design_project.id "
                            + "AND pm.user_id = {0} AND pm.is_delete = 0)", loginUser.getId()));
        }
        wrapper.and(StrUtil.isNotBlank(request.getKeyword()), nested -> nested
                .like("project_name", request.getKeyword())
                .or().like("project_code", request.getKeyword()));
        wrapper.eq(ObjUtil.isNotNull(request.getId()), "id", request.getId());
        wrapper.like(StrUtil.isNotBlank(request.getProjectCode()),
                "project_code", request.getProjectCode());
        wrapper.like(StrUtil.isNotBlank(request.getProjectName()),
                "project_name", request.getProjectName());
        wrapper.eq(StrUtil.isNotBlank(request.getCategory()), "category", request.getCategory());
        wrapper.eq(StrUtil.isNotBlank(request.getSeason()), "season", request.getSeason());
        wrapper.eq(StrUtil.isNotBlank(request.getPriority()), "priority", request.getPriority());
        wrapper.eq(StrUtil.isNotBlank(request.getStatus()), "status", request.getStatus());
        wrapper.eq(isAdmin(loginUser) && ObjUtil.isNotNull(request.getManagerId()),
                "manager_id", request.getManagerId());
        wrapper.ge(request.getPlanStartFrom() != null,
                "plan_start_date", request.getPlanStartFrom());
        wrapper.le(request.getPlanEndTo() != null,
                "plan_end_date", request.getPlanEndTo());

        Map<String, String> sortMap = Map.of(
                "id", "id", "projectCode", "project_code",
                "projectName", "project_name", "priority", "priority",
                "status", "status", "progress", "progress",
                "planStartDate", "plan_start_date", "planEndDate", "plan_end_date",
                "createTime", "create_time", "updateTime", "update_time"
        );
        String sortColumn = StrUtil.isNotBlank(request.getSortField())
                ? sortMap.get(request.getSortField()) : null;
        if (sortColumn != null) {
            wrapper.orderBy(true, "ascend".equals(request.getSortOrder()), sortColumn);
        } else {
            wrapper.orderByDesc("id");
        }
        return wrapper;
    }

    @Override
    public ProjectVO getProjectVO(DesignProject project) {
        if (project == null) return null;
        User manager = userMapper.selectById(project.getManagerId());
        User acceptor = project.getAcceptorId() == null ? null
                : userMapper.selectById(project.getAcceptorId());
        return convert(project, manager == null ? null : manager.getUserName(),
                acceptor == null ? null : acceptor.getUserName());
    }

    @Override
    public Page<ProjectVO> getProjectVOPage(Page<DesignProject> projectPage) {
        Page<ProjectVO> result = new Page<>(projectPage.getCurrent(), projectPage.getSize(),
                projectPage.getTotal());
        if (projectPage.getRecords().isEmpty()) {
            result.setRecords(new ArrayList<>());
            return result;
        }
        List<Long> userIds = projectPage.getRecords().stream()
                .flatMap(project -> java.util.stream.Stream.of(
                        project.getManagerId(), project.getAcceptorId()))
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        result.setRecords(projectPage.getRecords().stream()
                .map(project -> convert(project,
                        userMap.get(project.getManagerId()) == null ? null
                                : userMap.get(project.getManagerId()).getUserName(),
                        userMap.get(project.getAcceptorId()) == null ? null
                                : userMap.get(project.getAcceptorId()).getUserName()))
                .toList());
        return result;
    }

    private ProjectVO convert(DesignProject project, String managerName, String acceptorName) {
        ProjectVO vo = new ProjectVO();
        BeanUtil.copyProperties(project, vo);
        vo.setId(String.valueOf(project.getId()));
        vo.setManagerId(String.valueOf(project.getManagerId()));
        vo.setManagerName(managerName);
        if (project.getAcceptorId() != null) {
            vo.setAcceptorId(String.valueOf(project.getAcceptorId()));
        }
        vo.setAcceptorName(acceptorName);
        return vo;
    }

    @Override
    public DesignProject getAccessibleProject(Long projectId, User loginUser) {
        ThrowUtils.throwIf(projectId == null || projectId <= 0, ErrorCode.PARAMS_ERROR);
        DesignProject project = this.getById(projectId);
        ThrowUtils.throwIf(project == null, ErrorCode.NOT_FOUND_ERROR, "项目不存在");
        ThrowUtils.throwIf(!isAdmin(loginUser)
                        && !project.getManagerId().equals(loginUser.getId())
                        && !loginUser.getId().equals(project.getAcceptorId())
                        && countActiveMember(projectId, loginUser.getId()) == 0,
                ErrorCode.NO_AUTH_ERROR, "无权访问该项目");
        return project;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(ProjectUpdateRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null || request.getId() == null
                        || request.getVersion() == null,
                ErrorCode.PARAMS_ERROR, "项目 ID 和版本号不能为空");
        DesignProject old = getAccessibleProject(request.getId(), loginUser);
        ThrowUtils.throwIf(ProjectStatusEnum.COMPLETED.getValue().equals(old.getStatus())
                        || ProjectStatusEnum.CANCELLED.getValue().equals(old.getStatus()),
                ErrorCode.OPERATION_ERROR, "已完成或已取消项目不能修改");
        DesignProject effective = new DesignProject();
        BeanUtil.copyProperties(old, effective);
        BeanUtil.copyProperties(request, effective,
                CopyOptions.create().ignoreNullValue());
        effective.setProjectCode(old.getProjectCode());
        effective.setStatus(old.getStatus());
        effective.setUpdateBy(loginUser.getId());
        ThrowUtils.throwIf(ProjectStatusEnum.ACCEPTANCE.getValue().equals(old.getStatus())
                        && !java.util.Objects.equals(old.getAcceptorId(), effective.getAcceptorId()),
                ErrorCode.OPERATION_ERROR, "项目处于验收中，不能更换验收人");
        validProject(effective, false);
        ThrowUtils.throwIf(!this.updateById(effective), ErrorCode.OPERATION_ERROR,
                "项目已被其他用户修改，请刷新后重试");
        ensureManagerMember(effective.getId(), effective.getManagerId(), loginUser.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long projectId, Integer version, String expectedStatus,
                           String targetStatus, String reason, User loginUser) {
        ThrowUtils.throwIf(version == null, ErrorCode.PARAMS_ERROR, "版本号不能为空");
        DesignProject project = getAccessibleProject(projectId, loginUser);
        ThrowUtils.throwIf(!expectedStatus.equals(project.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前项目状态不允许执行此操作");

        if (ProjectStatusEnum.APPROVED.getValue().equals(targetStatus)) {
            ThrowUtils.throwIf(StrUtil.hasBlank(project.getProjectName(), project.getCategory())
                            || project.getPlanStartDate() == null || project.getPlanEndDate() == null,
                    ErrorCode.PARAMS_ERROR, "提交立项前必须填写名称、品类和计划日期");
        } else if (ProjectStatusEnum.DESIGNING.getValue().equals(targetStatus)
                && ProjectStatusEnum.ACCEPTANCE.getValue().equals(expectedStatus)) {
            ThrowUtils.throwIf(StrUtil.isBlank(reason), ErrorCode.PARAMS_ERROR,
                    "验收退回原因不能为空");
        } else if (ProjectStatusEnum.DESIGNING.getValue().equals(targetStatus)) {
            validateManager(project.getManagerId());
            Long memberCount = projectMemberMapper.selectCount(new QueryWrapper<ProjectMember>()
                    .eq("project_id", project.getId()));
            ThrowUtils.throwIf(memberCount == null || memberCount == 0,
                    ErrorCode.OPERATION_ERROR, "至少配置一名项目成员后才能开始设计");
        } else if (ProjectStatusEnum.ACCEPTANCE.getValue().equals(targetStatus)) {
            ThrowUtils.throwIf(!project.getManagerId().equals(loginUser.getId()),
                    ErrorCode.NO_AUTH_ERROR, "只有项目负责人可以提交项目验收");
            ThrowUtils.throwIf(project.getAcceptorId() == null,
                    ErrorCode.OPERATION_ERROR, "请先指定项目验收人");
            validateAcceptor(project.getAcceptorId());
            ThrowUtils.throwIf(project.getAcceptorId().equals(project.getManagerId()),
                    ErrorCode.OPERATION_ERROR, "项目负责人不能验收自己的项目");
            Long taskCount = designTaskMapper.selectCount(new QueryWrapper<DesignTask>()
                    .eq("project_id", project.getId())
                    .ne("status", TaskStatusEnum.CANCELLED.getValue()));
            Long unfinishedCount = designTaskMapper.selectCount(new QueryWrapper<DesignTask>()
                    .eq("project_id", project.getId())
                    .notIn("status", List.of(TaskStatusEnum.COMPLETED.getValue(),
                            TaskStatusEnum.CANCELLED.getValue())));
            ThrowUtils.throwIf(taskCount == null || taskCount == 0,
                    ErrorCode.OPERATION_ERROR, "至少创建一项设计任务后才能提交验收");
            ThrowUtils.throwIf(unfinishedCount != null && unfinishedCount > 0,
                    ErrorCode.OPERATION_ERROR, "全部设计任务完成后才能提交项目验收");
        } else if (ProjectStatusEnum.CANCELLED.getValue().equals(targetStatus)) {
            ThrowUtils.throwIf(StrUtil.isBlank(reason), ErrorCode.PARAMS_ERROR,
                    "取消项目必须填写原因");
        }

        DesignProject update = new DesignProject();
        update.setId(project.getId());
        update.setVersion(version);
        update.setStatus(targetStatus);
        update.setUpdateBy(loginUser.getId());
        if (ProjectStatusEnum.COMPLETED.getValue().equals(targetStatus)) update.setProgress(100);
        if (ProjectStatusEnum.DESIGNING.getValue().equals(targetStatus)
                && ProjectStatusEnum.ACCEPTANCE.getValue().equals(expectedStatus)) {
            update.setLastRejectionReason(StrUtil.trim(reason));
        }
        if (ProjectStatusEnum.CANCELLED.getValue().equals(targetStatus)) {
            update.setCancelReason(StrUtil.trim(reason));
        }
        ThrowUtils.throwIf(!this.updateById(update), ErrorCode.OPERATION_ERROR,
                "项目已被其他用户修改，请刷新后重试");
        if (ProjectStatusEnum.ACCEPTANCE.getValue().equals(targetStatus)) {
            notificationService.send(project.getAcceptorId(), "acceptance", "项目等待验收",
                    "“" + project.getProjectName() + "”已由项目负责人提交验收，请及时处理。",
                    "project", project.getId(), "/projects",
                    "acceptance-submit:" + project.getId() + ":" + version);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewAcceptance(Long projectId, Integer version, boolean approved,
                                 String opinion, String requestNo, User loginUser) {
        ThrowUtils.throwIf(projectId == null || projectId <= 0 || version == null,
                ErrorCode.PARAMS_ERROR, "项目 ID 和版本号不能为空");
        opinion = StrUtil.trim(opinion);
        requestNo = StrUtil.trim(requestNo);
        ThrowUtils.throwIf(StrUtil.isBlank(requestNo) || requestNo.length() > 64
                        || !requestNo.matches("^[A-Za-z0-9_-]+$"),
                ErrorCode.PARAMS_ERROR, "幂等请求号不合法");
        ThrowUtils.throwIf(!approved && StrUtil.isBlank(opinion),
                ErrorCode.PARAMS_ERROR, "验收退回原因不能为空");
        ThrowUtils.throwIf(StrUtil.length(opinion) > 500,
                ErrorCode.PARAMS_ERROR, "验收意见不能超过 500 个字符");

        String result = approved ? ProjectAcceptanceResultEnum.APPROVED.getValue()
                : ProjectAcceptanceResultEnum.REJECTED.getValue();
        ProjectAcceptanceRecord existing = projectAcceptanceRecordMapper.selectOne(
                new QueryWrapper<ProjectAcceptanceRecord>()
                        .eq("request_no", requestNo).last("LIMIT 1"));
        if (existing != null) {
            ThrowUtils.throwIf(!existing.getProjectId().equals(projectId)
                            || !existing.getAcceptorId().equals(loginUser.getId())
                            || !existing.getResult().equals(result),
                    ErrorCode.OPERATION_ERROR, "该幂等请求号已用于其他验收操作");
            return;
        }

        DesignProject project = getAccessibleProject(projectId, loginUser);
        ThrowUtils.throwIf(project.getAcceptorId() == null
                        || !project.getAcceptorId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只有项目指定验收人可以执行验收");
        ThrowUtils.throwIf(!ProjectStatusEnum.ACCEPTANCE.getValue().equals(project.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前项目不处于验收中状态");

        transition(projectId, version, ProjectStatusEnum.ACCEPTANCE.getValue(),
                approved ? ProjectStatusEnum.COMPLETED.getValue()
                        : ProjectStatusEnum.DESIGNING.getValue(),
                opinion, loginUser);

        ProjectAcceptanceRecord record = new ProjectAcceptanceRecord();
        record.setProjectId(projectId);
        record.setProjectVersion(version);
        record.setAcceptorId(loginUser.getId());
        record.setResult(result);
        record.setOpinion(opinion);
        record.setRequestNo(requestNo);
        ThrowUtils.throwIf(projectAcceptanceRecordMapper.insert(record) != 1,
                ErrorCode.OPERATION_ERROR, "保存项目验收记录失败");
        notificationService.send(project.getManagerId(), "acceptance",
                approved ? "项目验收已通过" : "项目验收被退回",
                approved ? "“" + project.getProjectName() + "”已通过验收。"
                        : "“" + project.getProjectName() + "”验收被退回："
                        + StrUtil.blankToDefault(opinion, "请按验收意见调整"),
                "project", project.getId(), "/projects", "acceptance-result:" + requestNo);
    }

    @Override
    public List<ProjectAcceptanceRecordVO> listAcceptanceHistory(Long projectId, User loginUser) {
        getAccessibleProject(projectId, loginUser);
        List<ProjectAcceptanceRecord> records = projectAcceptanceRecordMapper.selectList(
                new QueryWrapper<ProjectAcceptanceRecord>()
                        .eq("project_id", projectId).orderByDesc("create_time", "id"));
        if (records.isEmpty()) return new ArrayList<>();
        Map<Long, User> users = userMapper.selectByIds(records.stream()
                        .map(ProjectAcceptanceRecord::getAcceptorId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return records.stream().map(record -> {
            ProjectAcceptanceRecordVO vo = new ProjectAcceptanceRecordVO();
            BeanUtil.copyProperties(record, vo);
            vo.setId(String.valueOf(record.getId()));
            vo.setProjectId(String.valueOf(record.getProjectId()));
            vo.setAcceptorId(String.valueOf(record.getAcceptorId()));
            User user = users.get(record.getAcceptorId());
            vo.setAcceptorName(user == null ? null : user.getUserName());
            return vo;
        }).toList();
    }

    @Override
    public List<UserVO> listAcceptorCandidates(String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_status", 1)
                .apply("EXISTS (SELECT 1 FROM user_role ur INNER JOIN sys_role sr "
                        + "ON sr.id = ur.role_id AND sr.is_delete = 0 AND sr.status = 1 "
                        + "WHERE ur.user_id = `user`.id AND sr.role_code = 'project_acceptor')")
                .and(StrUtil.isNotBlank(keyword), nested -> nested.like("user_name", keyword)
                        .or().like("user_account", keyword))
                .orderByAsc("user_name").last("LIMIT 100");
        return userService.getUserVOList(userMapper.selectList(wrapper));
    }

    private boolean isAdmin(User user) {
        return user != null && userRoleMapper.selectRoleCodesByUserId(user.getId()).contains("admin");
    }

    private long countActiveMember(Long projectId, Long userId) {
        Long count = projectMemberMapper.selectCount(new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId).eq("user_id", userId));
        return count == null ? 0 : count;
    }

    private void ensureManagerMember(Long projectId, Long managerId, Long operatorId) {
        ProjectMember member = projectMemberMapper.selectOne(new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId).eq("user_id", managerId).last("LIMIT 1"));
        if (member == null) {
            member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(managerId);
            member.setProjectRole(ProjectMemberRoleEnum.MANAGER.getValue());
            member.setCreateBy(operatorId);
            ThrowUtils.throwIf(projectMemberMapper.insert(member) != 1,
                    ErrorCode.OPERATION_ERROR, "同步项目负责人失败");
        } else if (!ProjectMemberRoleEnum.MANAGER.getValue().equals(member.getProjectRole())) {
            member.setProjectRole(ProjectMemberRoleEnum.MANAGER.getValue());
            ThrowUtils.throwIf(projectMemberMapper.updateById(member) != 1,
                    ErrorCode.OPERATION_ERROR, "同步项目负责人失败");
        }
    }
}
