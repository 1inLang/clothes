package com.lsp.clothes.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lsp.clothes.mapper.DesignFileMapper;
import com.lsp.clothes.mapper.DesignTaskMapper;
import com.lsp.clothes.mapper.ProjectAcceptanceRecordMapper;
import com.lsp.clothes.mapper.ReviewRecordMapper;
import com.lsp.clothes.mapper.UserMapper;
import com.lsp.clothes.model.dto.project.ProjectQueryRequest;
import com.lsp.clothes.model.entity.DesignFile;
import com.lsp.clothes.model.entity.DesignProject;
import com.lsp.clothes.model.entity.DesignTask;
import com.lsp.clothes.model.entity.ProjectAcceptanceRecord;
import com.lsp.clothes.model.entity.ReviewRecord;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.enums.ProjectStatusEnum;
import com.lsp.clothes.model.enums.TaskStatusEnum;
import com.lsp.clothes.model.vo.dashboard.DashboardActivityVO;
import com.lsp.clothes.model.vo.dashboard.DashboardProjectItemVO;
import com.lsp.clothes.model.vo.dashboard.DashboardProjectStatusVO;
import com.lsp.clothes.model.vo.dashboard.DashboardStatusCountVO;
import com.lsp.clothes.model.vo.dashboard.DashboardSummaryVO;
import com.lsp.clothes.model.vo.dashboard.DashboardTaskCompletionVO;
import com.lsp.clothes.model.vo.dashboard.DashboardTodoVO;
import com.lsp.clothes.service.DashboardService;
import com.lsp.clothes.service.DesignProjectService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final Set<String> ASSIGNEE_TODO_STATUSES = Set.of(
            TaskStatusEnum.PENDING_ACCEPTANCE.getValue(),
            TaskStatusEnum.IN_PROGRESS.getValue(),
            TaskStatusEnum.REVISION.getValue());
    private static final Set<String> TERMINAL_TASK_STATUSES = Set.of(
            TaskStatusEnum.COMPLETED.getValue(), TaskStatusEnum.CANCELLED.getValue());

    @Resource
    private DesignProjectService designProjectService;
    @Resource
    private DesignTaskMapper designTaskMapper;
    @Resource
    private DesignFileMapper designFileMapper;
    @Resource
    private ReviewRecordMapper reviewRecordMapper;
    @Resource
    private ProjectAcceptanceRecordMapper projectAcceptanceRecordMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    public DashboardSummaryVO getSummary(User loginUser) {
        List<DashboardTodoVO> todos = buildTodos(loginUser);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueBoundary = now.plusDays(3);
        DashboardSummaryVO result = new DashboardSummaryVO();
        result.setMyTodoCount(todos.size());
        result.setPendingReviewCount((int) todos.stream()
                .filter(item -> "review".equals(item.getBusinessType())).count());
        result.setPendingAcceptanceCount((int) todos.stream()
                .filter(item -> "acceptance".equals(item.getBusinessType())).count());
        result.setDueSoonCount((int) todos.stream().filter(item -> item.getDeadline() != null
                && !item.getDeadline().isBefore(now)
                && !item.getDeadline().isAfter(dueBoundary)).count());
        result.setOverdueCount((int) todos.stream()
                .filter(item -> Boolean.TRUE.equals(item.getOverdue())).count());
        return result;
    }

    @Override
    public List<DashboardTodoVO> getMyTodos(User loginUser, int limit) {
        return buildTodos(loginUser).stream().limit(limit).toList();
    }

    @Override
    public DashboardProjectStatusVO getProjectStatus(User loginUser) {
        List<DesignProject> projects = accessibleProjects(loginUser);
        List<Long> projectIds = projects.stream().map(DesignProject::getId).toList();
        List<DesignTask> tasks = tasksForProjects(projectIds);
        Map<Long, List<DesignTask>> tasksByProject = tasks.stream()
                .collect(Collectors.groupingBy(DesignTask::getProjectId));

        Map<String, Long> counts = projects.stream().collect(Collectors.groupingBy(
                DesignProject::getStatus, LinkedHashMap::new, Collectors.counting()));
        List<DashboardStatusCountVO> distribution = List.of(
                ProjectStatusEnum.DRAFT, ProjectStatusEnum.APPROVED,
                ProjectStatusEnum.DESIGNING, ProjectStatusEnum.ACCEPTANCE,
                ProjectStatusEnum.COMPLETED, ProjectStatusEnum.CANCELLED).stream()
                .map(status -> new DashboardStatusCountVO(status.getValue(),
                        counts.getOrDefault(status.getValue(), 0L))).toList();

        List<DashboardProjectItemVO> items = projects.stream()
                .sorted(Comparator.comparing(DesignProject::getUpdateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).map(project -> {
                    List<DesignTask> projectTasks = tasksByProject
                            .getOrDefault(project.getId(), List.of()).stream()
                            .filter(task -> !TaskStatusEnum.CANCELLED.getValue()
                                    .equals(task.getStatus())).toList();
                    DashboardProjectItemVO item = new DashboardProjectItemVO();
                    item.setId(String.valueOf(project.getId()));
                    item.setProjectCode(project.getProjectCode());
                    item.setProjectName(project.getProjectName());
                    item.setCategory(project.getCategory());
                    item.setStatus(project.getStatus());
                    item.setProgress(project.getProgress());
                    item.setTotalTasks((long) projectTasks.size());
                    item.setCompletedTasks(projectTasks.stream().filter(task ->
                            TaskStatusEnum.COMPLETED.getValue().equals(task.getStatus())).count());
                    return item;
                }).toList();
        DashboardProjectStatusVO result = new DashboardProjectStatusVO();
        result.setDistribution(distribution);
        result.setProjects(items);
        return result;
    }

    @Override
    public DashboardTaskCompletionVO getTaskCompletion(User loginUser) {
        List<DesignTask> tasks = tasksForProjects(accessibleProjectIds(loginUser)).stream()
                .filter(task -> !TaskStatusEnum.CANCELLED.getValue().equals(task.getStatus()))
                .toList();
        long completed = tasks.stream().filter(task ->
                TaskStatusEnum.COMPLETED.getValue().equals(task.getStatus())).count();
        long overdue = tasks.stream().filter(task -> task.getDeadline() != null
                && task.getDeadline().isBefore(LocalDateTime.now())
                && !TERMINAL_TASK_STATUSES.contains(task.getStatus())).count();
        DashboardTaskCompletionVO result = new DashboardTaskCompletionVO();
        result.setTotal((long) tasks.size());
        result.setCompleted(completed);
        result.setInProgress(tasks.size() - completed);
        result.setOverdue(overdue);
        result.setCompletionRate(tasks.isEmpty() ? 0
                : (int) Math.round(completed * 100.0 / tasks.size()));
        return result;
    }

    @Override
    public List<DashboardActivityVO> getRecentActivities(User loginUser, int limit) {
        List<DesignProject> projects = accessibleProjects(loginUser);
        if (projects.isEmpty()) return new ArrayList<>();
        List<Long> projectIds = projects.stream().map(DesignProject::getId).toList();
        Map<Long, DesignProject> projectMap = projects.stream()
                .collect(Collectors.toMap(DesignProject::getId, Function.identity()));
        List<DesignTask> tasks = tasksForProjects(projectIds);
        Map<Long, DesignTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(DesignTask::getId, Function.identity()));
        List<Long> taskIds = tasks.stream().map(DesignTask::getId).toList();

        List<DesignFile> files = designFileMapper.selectList(new QueryWrapper<DesignFile>()
                .in("project_id", projectIds).orderByDesc("create_time").last("LIMIT 30"));
        List<ReviewRecord> reviews = taskIds.isEmpty() ? List.of()
                : reviewRecordMapper.selectList(new QueryWrapper<ReviewRecord>()
                .in("task_id", taskIds).orderByDesc("create_time").last("LIMIT 30"));
        List<ProjectAcceptanceRecord> acceptances = projectAcceptanceRecordMapper.selectList(
                new QueryWrapper<ProjectAcceptanceRecord>().in("project_id", projectIds)
                        .orderByDesc("create_time").last("LIMIT 30"));

        List<Long> actorIds = new ArrayList<>();
        files.forEach(item -> actorIds.add(item.getUploaderId()));
        reviews.forEach(item -> actorIds.add(item.getReviewerId()));
        acceptances.forEach(item -> actorIds.add(item.getAcceptorId()));
        List<Long> distinctActorIds = actorIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, User> users = distinctActorIds.isEmpty() ? new HashMap<>()
                : userMapper.selectByIds(distinctActorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<DashboardActivityVO> activities = new ArrayList<>();
        files.forEach(file -> {
            DesignTask task = taskMap.get(file.getTaskId());
            DesignProject project = projectMap.get(file.getProjectId());
            String action = "design".equals(file.getFileType())
                    ? "上传了设计稿 V" + file.getVersionNo() : "上传了项目附件";
            activities.add(activity("file-" + file.getId(), "file", users.get(file.getUploaderId()),
                    action, task == null ? projectName(project) : task.getTaskName(),
                    file.getCreateTime(), "blue", "/tasks"));
        });
        reviews.forEach(review -> {
            DesignTask task = taskMap.get(review.getTaskId());
            boolean approved = "approved".equals(review.getResult());
            activities.add(activity("review-" + review.getId(), "review",
                    users.get(review.getReviewerId()), approved ? "通过了设计审核" : "退回了设计稿修改",
                    task == null ? "设计任务" : task.getTaskName(), review.getCreateTime(),
                    approved ? "green" : "coral", "/reviews"));
        });
        acceptances.forEach(record -> {
            DesignProject project = projectMap.get(record.getProjectId());
            boolean approved = "approved".equals(record.getResult());
            activities.add(activity("acceptance-" + record.getId(), "acceptance",
                    users.get(record.getAcceptorId()), approved ? "通过了项目验收" : "退回了项目验收",
                    projectName(project), record.getCreateTime(), approved ? "green" : "amber",
                    "/projects"));
        });
        return activities.stream().sorted(Comparator.comparing(DashboardActivityVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder()))).limit(limit).toList();
    }

    private List<DashboardTodoVO> buildTodos(User loginUser) {
        List<DesignProject> projects = accessibleProjects(loginUser);
        if (projects.isEmpty()) return new ArrayList<>();
        Map<Long, DesignProject> projectMap = projects.stream()
                .collect(Collectors.toMap(DesignProject::getId, Function.identity()));
        List<DashboardTodoVO> todos = new ArrayList<>();
        for (DesignTask task : tasksForProjects(new ArrayList<>(projectMap.keySet()))) {
            DesignProject project = projectMap.get(task.getProjectId());
            if (loginUser.getId().equals(task.getReviewerId())
                    && TaskStatusEnum.PENDING_REVIEW.getValue().equals(task.getStatus())) {
                todos.add(taskTodo(task, project, "review", "待审核", "/reviews"));
            } else if (loginUser.getId().equals(task.getAssigneeId())
                    && ASSIGNEE_TODO_STATUSES.contains(task.getStatus())) {
                todos.add(taskTodo(task, project, "task", task.getStatus(), "/tasks"));
            }
        }
        projects.stream().filter(project -> loginUser.getId().equals(project.getAcceptorId())
                && ProjectStatusEnum.ACCEPTANCE.getValue().equals(project.getStatus()))
                .forEach(project -> {
                    DashboardTodoVO todo = new DashboardTodoVO();
                    todo.setId(String.valueOf(project.getId()));
                    todo.setBusinessType("acceptance");
                    todo.setTitle(project.getProjectName());
                    todo.setSubtitle(project.getProjectCode() + " · 项目验收");
                    todo.setStatus("验收中");
                    todo.setPriority(project.getPriority());
                    todo.setDeadline(project.getPlanEndDate() == null ? null
                            : LocalDateTime.of(project.getPlanEndDate(), LocalTime.MAX));
                    todo.setRoute("/projects");
                    todo.setOverdue(todo.getDeadline() != null
                            && todo.getDeadline().isBefore(LocalDateTime.now()));
                    todos.add(todo);
                });
        return todos.stream().sorted(todoComparator()).toList();
    }

    private DashboardTodoVO taskTodo(DesignTask task, DesignProject project,
                                     String businessType, String status, String route) {
        DashboardTodoVO todo = new DashboardTodoVO();
        todo.setId(String.valueOf(task.getId()));
        todo.setBusinessType(businessType);
        todo.setTitle(task.getTaskName());
        todo.setSubtitle(projectName(project) + " · " + task.getTaskCode());
        todo.setStatus(status);
        todo.setPriority(task.getPriority());
        todo.setDeadline(task.getDeadline());
        todo.setOverdue(task.getDeadline() != null
                && task.getDeadline().isBefore(LocalDateTime.now()));
        todo.setRoute(route);
        return todo;
    }

    private Comparator<DashboardTodoVO> todoComparator() {
        Map<String, Integer> priority = Map.of("high", 0, "medium", 1, "low", 2);
        return Comparator.comparing(DashboardTodoVO::getOverdue,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> item.getDeadline() == null
                        ? LocalDateTime.MAX : item.getDeadline())
                .thenComparing(item -> priority.getOrDefault(item.getPriority(), 3));
    }

    private DashboardActivityVO activity(String id, String type, User actor, String action,
                                         String subject, Date createTime, String tone, String route) {
        DashboardActivityVO item = new DashboardActivityVO();
        item.setId(id);
        item.setType(type);
        item.setActorName(actor == null ? "系统用户" : actor.getUserName());
        item.setAction(action);
        item.setSubject(subject);
        item.setCreateTime(createTime);
        item.setTone(tone);
        item.setRoute(route);
        return item;
    }

    private List<DesignProject> accessibleProjects(User loginUser) {
        return designProjectService.list(designProjectService.getQueryWrapper(
                new ProjectQueryRequest(), loginUser));
    }

    private List<Long> accessibleProjectIds(User loginUser) {
        return accessibleProjects(loginUser).stream().map(DesignProject::getId).toList();
    }

    private List<DesignTask> tasksForProjects(List<Long> projectIds) {
        if (projectIds.isEmpty()) return new ArrayList<>();
        return designTaskMapper.selectList(new QueryWrapper<DesignTask>()
                .in("project_id", projectIds));
    }

    private String projectName(DesignProject project) {
        return project == null ? "设计项目" : project.getProjectName();
    }
}
