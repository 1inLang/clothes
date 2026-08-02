-- 项目成员模块数据库脚本
-- 前置条件：已执行 rbac_schema.sql 和 project_schema.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `project_member`
(
    `id`           BIGINT      NOT NULL COMMENT '成员关系ID（雪花ID）',
    `project_id`   BIGINT      NOT NULL COMMENT '项目ID',
    `user_id`      BIGINT      NOT NULL COMMENT '用户ID',
    `project_role` VARCHAR(30) NOT NULL COMMENT '项目角色：manager/designer/reviewer/viewer',
    `join_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_by`    BIGINT      NOT NULL COMMENT '操作人用户ID',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    `active_user_id` BIGINT GENERATED ALWAYS AS
        (CASE WHEN `is_delete` = 0 THEN `user_id` ELSE NULL END) STORED COMMENT '有效成员唯一键',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_active_member` (`project_id`, `active_user_id`),
    KEY `idx_project_member_project` (`project_id`, `is_delete`),
    KEY `idx_project_member_user` (`user_id`, `is_delete`),
    KEY `idx_project_member_role` (`project_id`, `project_role`, `is_delete`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '项目成员关系表';

-- 为已经存在的项目补齐负责人成员记录。
INSERT INTO `project_member` (`id`, `project_id`, `user_id`, `project_role`, `create_by`)
SELECT UUID_SHORT(), p.id, p.manager_id, 'manager', p.create_by
FROM `design_project` p
WHERE p.is_delete = 0
  AND NOT EXISTS (
      SELECT 1 FROM `project_member` pm
      WHERE pm.project_id = p.id AND pm.user_id = p.manager_id AND pm.is_delete = 0
  );

INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`,
     `sort_order`, `status`, `description`)
VALUES
    ('管理项目成员', 'project:member', 2,
     '/project/member/list,/project/member/add,/project/member/update,/project/member/remove,/project/member/candidates',
     110, 1, '维护项目成员及项目内角色');

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code = 'project:member'
WHERE r.role_code IN ('admin', 'project_manager')
  AND r.is_delete = 0 AND p.is_delete = 0;

-- 普通用户作为只读成员时也需要基础项目查看权限，实际数据仍由成员关系过滤。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code = 'project:view'
WHERE r.role_code = 'user'
  AND r.is_delete = 0 AND p.is_delete = 0;

SELECT p.permission_code, GROUP_CONCAT(r.role_code ORDER BY r.role_code) AS roles
FROM `sys_permission` p
LEFT JOIN `sys_role_permission` rp ON rp.permission_id = p.id
LEFT JOIN `sys_role` r ON r.id = rp.role_id
WHERE p.permission_code = 'project:member'
GROUP BY p.id, p.permission_code;
