-- 项目验收职责分离增量脚本
-- 适用于已经执行过 project_schema.sql 的数据库；请只执行一次。
-- 数据库：MySQL 8.x

SET NAMES utf8mb4;

ALTER TABLE `design_project`
    ADD COLUMN `acceptor_id` BIGINT DEFAULT NULL COMMENT '项目验收人用户ID' AFTER `manager_id`,
    ADD KEY `idx_project_acceptor` (`acceptor_id`, `status`, `is_delete`);

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

INSERT IGNORE INTO `sys_role`
    (`role_name`, `role_code`, `description`, `sort_order`, `status`)
VALUES
    ('项目验收人', 'project_acceptor', '验收指定的设计项目并填写验收意见', 35, 1);

UPDATE `sys_permission`
SET `permission_name` = '查看设计项目',
    `path` = '/project/list/page,/project/get,/project/progress,/project/acceptance/history',
    `description` = '查看有权访问的设计项目'
WHERE `permission_code` = 'project:view' AND `is_delete` = 0;

UPDATE `sys_permission`
SET `permission_name` = '更新设计项目',
    `path` = '/project/update,/project/submit,/project/start-design,/project/acceptance/candidates',
    `description` = '编辑项目并推进至设计阶段'
WHERE `permission_code` = 'project:update' AND `is_delete` = 0;

UPDATE `sys_permission`
SET `permission_name` = '取消设计项目',
    `path` = '/project/cancel',
    `description` = '取消尚未完成的项目'
WHERE `permission_code` = 'project:close' AND `is_delete` = 0;

INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`,
     `sort_order`, `status`, `description`)
VALUES
    ('提交项目验收', 'project:submit_acceptance', 2, '/project/submit-acceptance', 95, 1,
     '项目负责人提交项目验收'),
    ('审核项目验收', 'project:acceptance:review', 2, '/project/accept,/project/reject-acceptance', 98, 1,
     '指定验收人通过或退回项目验收');

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN
    ('project:view', 'project:create', 'project:update', 'project:submit_acceptance', 'project:close')
WHERE r.role_code = 'project_manager' AND r.is_delete = 0 AND p.is_delete = 0;

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN ('project:view', 'project:acceptance:review')
WHERE r.role_code = 'project_acceptor' AND r.is_delete = 0 AND p.is_delete = 0;

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN
    ('project:submit_acceptance', 'project:acceptance:review')
WHERE r.role_code = 'admin' AND r.is_delete = 0 AND p.is_delete = 0;

SELECT p.permission_code, GROUP_CONCAT(r.role_code ORDER BY r.role_code) AS roles
FROM `sys_permission` p
LEFT JOIN `sys_role_permission` rp ON rp.permission_id = p.id
LEFT JOIN `sys_role` r ON r.id = rp.role_id
WHERE p.permission_code LIKE 'project:%'
GROUP BY p.id, p.permission_code
ORDER BY p.sort_order;
