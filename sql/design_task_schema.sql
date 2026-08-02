-- 设计任务模块数据库脚本
-- 前置条件：已执行 project_schema.sql、project_member_schema.sql、user_role_migration.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `design_task`
(
    `id`               BIGINT       NOT NULL COMMENT '任务ID（雪花ID）',
    `project_id`       BIGINT       NOT NULL COMMENT '所属项目ID',
    `task_code`        VARCHAR(50)  NOT NULL COMMENT '任务编号',
    `task_name`        VARCHAR(120) NOT NULL COMMENT '任务名称',
    `requirement`      TEXT                  DEFAULT NULL COMMENT '任务要求',
    `assignee_id`      BIGINT                DEFAULT NULL COMMENT '任务负责人用户ID',
    `reviewer_id`      BIGINT                DEFAULT NULL COMMENT '审核人用户ID',
    `priority`         VARCHAR(20)  NOT NULL DEFAULT 'medium' COMMENT '优先级：low/medium/high',
    `status`           VARCHAR(30)  NOT NULL DEFAULT 'unassigned' COMMENT '任务状态',
    `deadline`         DATETIME              DEFAULT NULL COMMENT '截止时间',
    `progress`         INT          NOT NULL DEFAULT 0 COMMENT '任务进度：0-100',
    `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `last_submit_note` VARCHAR(500)          DEFAULT NULL COMMENT '最近提交审核说明',
    `rejection_reason` VARCHAR(500)          DEFAULT NULL COMMENT '最近退回原因',
    `cancel_reason`    VARCHAR(500)          DEFAULT NULL COMMENT '取消原因',
    `create_by`        BIGINT       NOT NULL COMMENT '创建人用户ID',
    `update_by`        BIGINT       NOT NULL COMMENT '最后更新人用户ID',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_design_task_code` (`task_code`),
    KEY `idx_task_project_status` (`project_id`, `status`, `is_delete`),
    KEY `idx_task_assignee_status` (`assignee_id`, `status`, `is_delete`),
    KEY `idx_task_reviewer_status` (`reviewer_id`, `status`, `is_delete`),
    KEY `idx_task_deadline` (`deadline`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设计任务表';

INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`, `sort_order`, `status`, `description`)
VALUES
    ('查看设计任务', 'task:view', 2, '/task/list/page,/task/my/page,/task/get', 120, 1, '查看有权访问的设计任务'),
    ('创建设计任务', 'task:create', 2, '/task/add,/task/update', 130, 1, '创建和编辑设计任务'),
    ('分派设计任务', 'task:assign', 2, '/task/assign,/task/cancel', 140, 1, '分派负责人、审核人及取消任务'),
    ('提交设计任务', 'task:submit', 2, '/task/accept,/task/update-progress,/task/submit-review', 150, 1, '领取任务、更新进度和提交审核');

-- 所有业务角色获得查看权限；具体数据范围由项目成员关系控制。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code = 'task:view'
WHERE r.role_code IN ('admin', 'project_manager', 'designer', 'reviewer', 'user')
  AND r.is_delete = 0 AND p.is_delete = 0;

-- 管理员和项目经理维护、分派任务。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN ('task:create', 'task:assign')
WHERE r.role_code IN ('admin', 'project_manager')
  AND r.is_delete = 0 AND p.is_delete = 0;

-- 是否能操作任务还会校验当前登录人是否为任务负责人。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code = 'task:submit'
WHERE r.role_code IN ('admin', 'project_manager', 'designer', 'reviewer', 'user')
  AND r.is_delete = 0 AND p.is_delete = 0;
