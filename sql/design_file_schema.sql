-- 设计稿与附件模块数据库脚本
-- 前置条件：已执行 design_task_schema.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `design_file`
(
    `id`           BIGINT        NOT NULL COMMENT '文件记录ID（雪花ID）',
    `project_id`   BIGINT        NOT NULL COMMENT '所属项目ID',
    `task_id`      BIGINT                 DEFAULT NULL COMMENT '所属任务ID',
    `file_name`    VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    `storage_key`  VARCHAR(255)  NOT NULL COMMENT '存储对象键',
    `file_type`    VARCHAR(30)   NOT NULL COMMENT '文件类型：design/reference/attachment',
    `mime_type`    VARCHAR(120)  NOT NULL COMMENT 'MIME类型',
    `file_size`    BIGINT        NOT NULL COMMENT '文件大小（字节）',
    `version_no`   INT           NOT NULL COMMENT '任务或项目范围内版本号',
    `version_note` VARCHAR(500)           DEFAULT NULL COMMENT '版本说明',
    `submitted_flag` TINYINT     NOT NULL DEFAULT 0 COMMENT '是否曾提交审核：0否，1是',
    `uploader_id`  BIGINT        NOT NULL COMMENT '上传人用户ID',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `is_delete`    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    `active_scope_id` BIGINT GENERATED ALWAYS AS
        (CASE WHEN `is_delete` = 0 THEN COALESCE(`task_id`, -`project_id`) ELSE NULL END) STORED COMMENT '有效版本唯一范围',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_design_file_storage` (`storage_key`),
    UNIQUE KEY `uk_design_file_active_version` (`active_scope_id`, `version_no`),
    KEY `idx_file_project` (`project_id`, `is_delete`),
    KEY `idx_file_task_type` (`task_id`, `file_type`, `is_delete`),
    KEY `idx_file_uploader` (`uploader_id`, `is_delete`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设计稿与附件版本表';

ALTER TABLE `design_task`
    ADD COLUMN `submitted_file_id` BIGINT DEFAULT NULL COMMENT '当前提交审核的设计稿文件ID'
        AFTER `version`,
    ADD KEY `idx_task_submitted_file` (`submitted_file_id`);

INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`, `sort_order`, `status`, `description`)
VALUES
    ('查看设计文件', 'design:view', 2, '/file/list,/file/version/list,/file/get', 160, 1, '查看项目设计稿和附件元数据'),
    ('上传设计文件', 'design:upload', 2, '/file/upload', 170, 1, '上传设计稿、参考资料和附件'),
    ('下载设计文件', 'design:download', 2, '/file/download', 180, 1, '下载有权限访问的文件'),
    ('删除设计文件', 'design:delete', 2, '/file/delete', 190, 1, '逻辑删除未进入审核流程的文件版本');

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN
    ('design:view', 'design:upload', 'design:download', 'design:delete')
WHERE r.role_code IN ('admin', 'project_manager', 'designer', 'reviewer', 'user')
  AND r.is_delete = 0 AND p.is_delete = 0;
