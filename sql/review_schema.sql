-- 审核与返修模块数据库脚本
-- 前置条件：已执行 design_file_schema.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `review_record`
(
    `id`          BIGINT       NOT NULL COMMENT '审核记录ID（雪花ID）',
    `task_id`     BIGINT       NOT NULL COMMENT '任务ID',
    `file_id`     BIGINT       NOT NULL COMMENT '本次审核的设计稿文件ID',
    `version_no`  INT          NOT NULL COMMENT '设计稿版本号',
    `reviewer_id` BIGINT       NOT NULL COMMENT '审核人用户ID',
    `result`      VARCHAR(20)  NOT NULL COMMENT '审核结果：approved/rejected',
    `opinion`     VARCHAR(1000)         DEFAULT NULL COMMENT '审核意见',
    `request_no`  VARCHAR(64)  NOT NULL COMMENT '客户端幂等请求号',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_request_no` (`request_no`),
    KEY `idx_review_task_time` (`task_id`, `create_time`),
    KEY `idx_review_file` (`file_id`),
    KEY `idx_review_reviewer` (`reviewer_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设计任务审核记录表';

INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`, `sort_order`, `status`, `description`)
VALUES
    ('审核设计任务', 'task:review', 2,
     '/review/pending/page,/review/detail,/review/history,/review/approve,/review/reject',
     200, 1, '查看待审核任务并执行通过或退回');

-- 项目内指定审核人才能真正执行审核，角色权限只开放接口入口。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code = 'task:review'
WHERE r.role_code IN ('admin', 'project_manager', 'designer', 'reviewer', 'user')
  AND r.is_delete = 0 AND p.is_delete = 0;
