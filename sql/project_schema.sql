-- 设计项目模块数据库脚本
-- 数据库：MySQL 8.x
-- 前置条件：已执行 rbac_schema.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `design_project`
(
    `id`                    BIGINT       NOT NULL COMMENT '项目ID（雪花ID）',
    `project_code`          VARCHAR(50)  NOT NULL COMMENT '项目编号',
    `project_name`          VARCHAR(100) NOT NULL COMMENT '项目名称',
    `category`              VARCHAR(50)           DEFAULT NULL COMMENT '服装品类',
    `season`                VARCHAR(50)           DEFAULT NULL COMMENT '企划季节',
    `style`                 VARCHAR(100)          DEFAULT NULL COMMENT '设计风格',
    `target_audience`       VARCHAR(100)          DEFAULT NULL COMMENT '目标人群',
    `requirement`           TEXT                  DEFAULT NULL COMMENT '设计需求',
    `manager_id`            BIGINT       NOT NULL COMMENT '项目负责人用户ID',
    `acceptor_id`           BIGINT                DEFAULT NULL COMMENT '项目验收人用户ID',
    `priority`              VARCHAR(20)  NOT NULL DEFAULT 'medium' COMMENT '优先级：low/medium/high',
    `status`                VARCHAR(30)  NOT NULL DEFAULT 'draft' COMMENT '状态：draft/approved/designing/acceptance/completed/cancelled',
    `plan_start_date`       DATE                  DEFAULT NULL COMMENT '计划开始日期',
    `plan_end_date`         DATE                  DEFAULT NULL COMMENT '计划结束日期',
    `progress`              INT          NOT NULL DEFAULT 0 COMMENT '整体进度：0-100',
    `version`               INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `last_rejection_reason` VARCHAR(500)          DEFAULT NULL COMMENT '最近一次验收退回原因',
    `cancel_reason`         VARCHAR(500)          DEFAULT NULL COMMENT '取消原因',
    `create_by`             BIGINT       NOT NULL COMMENT '创建人用户ID',
    `update_by`             BIGINT       NOT NULL COMMENT '最后更新人用户ID',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                   ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_code` (`project_code`),
    KEY `idx_project_manager` (`manager_id`, `is_delete`),
    KEY `idx_project_acceptor` (`acceptor_id`, `status`, `is_delete`),
    KEY `idx_project_status` (`status`, `is_delete`),
    KEY `idx_project_category_season` (`category`, `season`),
    KEY `idx_project_plan_end` (`plan_end_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设计项目表';

CREATE TABLE IF NOT EXISTS `project_acceptance_record`
(
    `id`              BIGINT      NOT NULL COMMENT '验收记录ID（雪花ID）',
    `project_id`      BIGINT      NOT NULL COMMENT '项目ID',
    `project_version` INT         NOT NULL COMMENT '验收操作对应的项目版本',
    `acceptor_id`     BIGINT      NOT NULL COMMENT '项目验收人用户ID',
    `result`          VARCHAR(20) NOT NULL COMMENT '结果：approved/rejected',
    `opinion`         VARCHAR(500)         DEFAULT NULL COMMENT '验收意见',
    `request_no`      VARCHAR(64) NOT NULL COMMENT '客户端幂等请求号',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '验收时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_acceptance_request_no` (`request_no`),
    KEY `idx_project_acceptance_project` (`project_id`, `create_time`),
    KEY `idx_project_acceptance_acceptor` (`acceptor_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '项目验收记录表';

INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`,
     `sort_order`, `status`, `description`)
VALUES
    ('查看设计项目', 'project:view', 2, '/project/list/page,/project/get,/project/progress,/project/acceptance/history', 70, 1, '查看有权访问的设计项目'),
    ('创建设计项目', 'project:create', 2, '/project/add', 80, 1, '创建新的设计项目'),
    ('更新设计项目', 'project:update', 2, '/project/update,/project/submit,/project/start-design,/project/acceptance/candidates', 90, 1, '编辑项目并推进至设计阶段'),
    ('提交项目验收', 'project:submit_acceptance', 2, '/project/submit-acceptance', 95, 1, '项目负责人提交项目验收'),
    ('审核项目验收', 'project:acceptance:review', 2, '/project/accept,/project/reject-acceptance', 98, 1, '指定验收人通过或退回项目验收'),
    ('取消设计项目', 'project:close', 2, '/project/cancel', 100, 1, '取消尚未完成的项目');

-- 系统管理员获得全部项目权限。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN
    ('project:view', 'project:create', 'project:update', 'project:submit_acceptance',
     'project:acceptance:review', 'project:close')
WHERE r.role_code = 'admin' AND r.is_delete = 0 AND p.is_delete = 0;

-- 项目经理获得全部项目业务权限。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN
    ('project:view', 'project:create', 'project:update', 'project:submit_acceptance', 'project:close')
WHERE r.role_code = 'project_manager' AND r.is_delete = 0 AND p.is_delete = 0;

-- 项目验收人只能查看并验收指定给自己的项目。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN
    ('project:view', 'project:acceptance:review')
WHERE r.role_code = 'project_acceptor' AND r.is_delete = 0 AND p.is_delete = 0;

-- 设计师和审核人预置项目查看权限，项目成员模块完成后再按成员关系过滤。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code = 'project:view'
WHERE r.role_code IN ('designer', 'reviewer')
  AND r.is_delete = 0 AND p.is_delete = 0;

SELECT p.permission_code, GROUP_CONCAT(r.role_code ORDER BY r.role_code) AS roles
FROM `sys_permission` p
LEFT JOIN `sys_role_permission` rp ON rp.permission_id = p.id
LEFT JOIN `sys_role` r ON r.id = rp.role_id
WHERE p.permission_code LIKE 'project:%'
GROUP BY p.id, p.permission_code
ORDER BY p.sort_order;
