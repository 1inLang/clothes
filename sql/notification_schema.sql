-- 站内通知模块数据库脚本
-- 数据库：MySQL 8.x；前置条件：已执行 rbac_schema.sql。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `system_notification`
(
    `id`            BIGINT       NOT NULL COMMENT '通知ID（雪花ID）',
    `receiver_id`   BIGINT       NOT NULL COMMENT '接收用户ID',
    `type`          VARCHAR(30)  NOT NULL COMMENT '类型：task/review/acceptance/project/deadline',
    `title`         VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content`       VARCHAR(500) NOT NULL COMMENT '通知内容',
    `business_type` VARCHAR(30)           DEFAULT NULL COMMENT '业务类型：task/project',
    `business_id`   BIGINT                DEFAULT NULL COMMENT '业务对象ID',
    `route`         VARCHAR(200)          DEFAULT NULL COMMENT '前端跳转路由',
    `read_flag`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0否，1是',
    `read_time`     DATETIME              DEFAULT NULL COMMENT '阅读时间',
    `biz_key`       VARCHAR(128) NOT NULL COMMENT '业务事件幂等键',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_biz_receiver` (`receiver_id`, `biz_key`),
    KEY `idx_notification_receiver_read` (`receiver_id`, `read_flag`, `create_time`),
    KEY `idx_notification_business` (`business_type`, `business_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '站内通知表';

INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`,
     `sort_order`, `status`, `description`)
VALUES
    ('查看站内通知', 'notification:view', 2,
     '/notification/list/page,/notification/unread-count', 180, 1,
     '查看当前登录用户自己的通知和未读数量'),
    ('更新通知状态', 'notification:update', 2,
     '/notification/read,/notification/read-all', 190, 1,
     '将当前登录用户自己的通知标记为已读');

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN ('notification:view', 'notification:update')
WHERE r.role_code IN ('user', 'designer', 'reviewer', 'project_acceptor', 'project_manager', 'admin')
  AND r.is_delete = 0 AND p.is_delete = 0;

SELECT p.permission_code, GROUP_CONCAT(r.role_code ORDER BY r.role_code) AS roles
FROM `sys_permission` p
LEFT JOIN `sys_role_permission` rp ON rp.permission_id = p.id
LEFT JOIN `sys_role` r ON r.id = rp.role_id
WHERE p.permission_code LIKE 'notification:%'
GROUP BY p.id, p.permission_code
ORDER BY p.sort_order;
