-- 工作台与统计模块权限增量脚本
-- 不新增业务数据表，仅新增接口权限；可重复执行。

SET NAMES utf8mb4;

INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`,
     `sort_order`, `status`, `description`)
VALUES
    ('查看工作台', 'dashboard:view', 2,
     '/dashboard/summary,/dashboard/my-todos,/dashboard/project-status,/dashboard/task-completion,/dashboard/recent-activities',
     170, 1, '查看当前用户数据权限范围内的工作台统计与动态');

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code = 'dashboard:view'
WHERE r.role_code IN ('user', 'designer', 'reviewer', 'project_acceptor', 'project_manager', 'admin')
  AND r.is_delete = 0 AND p.is_delete = 0;

SELECT p.permission_code, GROUP_CONCAT(r.role_code ORDER BY r.role_code) AS roles
FROM `sys_permission` p
LEFT JOIN `sys_role_permission` rp ON rp.permission_id = p.id
LEFT JOIN `sys_role` r ON r.id = rp.role_id
WHERE p.permission_code = 'dashboard:view'
GROUP BY p.id, p.permission_code;
