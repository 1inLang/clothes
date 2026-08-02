-- 服装设计业务流程管理系统 RBAC 表结构
-- 数据库：MySQL 8.x
-- 说明：
-- 1. 当前 user.user_role 保存单个角色编码，因此暂不创建 user_role 关联表。
-- 2. 角色编码必须与 UserRoleEnum 中的 value 保持一致。
-- 3. 执行本文件只会创建/初始化 RBAC 表，不会修改现有 user 表。

SET NAMES utf8mb4;

-- 用户与角色现已改为多对多关系；请同时执行 user_role_migration.sql。
-- 本文件中旧 user.user_role 示例仅用于历史说明，不再作为当前实现依据。

-- ----------------------------
-- 1. 角色表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`   VARCHAR(50)     NOT NULL COMMENT '角色名称',
    `role_code`   VARCHAR(50)     NOT NULL COMMENT '角色编码',
    `description` VARCHAR(255)             DEFAULT NULL COMMENT '角色说明',
    `sort_order`  INT             NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_code` (`role_code`),
    KEY `idx_sys_role_status` (`status`, `is_delete`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '系统角色表';

-- ----------------------------
-- 2. 权限表
-- permission_type：
-- 1 菜单权限，2 按钮/接口权限
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_permission`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `permission_name` VARCHAR(100)    NOT NULL COMMENT '权限名称',
    `permission_code` VARCHAR(100)    NOT NULL COMMENT 'Sa-Token权限编码',
    `permission_type` TINYINT         NOT NULL DEFAULT 2 COMMENT '类型：1菜单，2按钮/接口',
    `parent_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限ID，0表示顶级',
    `path`            VARCHAR(255)             DEFAULT NULL COMMENT '前端路由或接口路径',
    `sort_order`      INT             NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    `description`     VARCHAR(255)             DEFAULT NULL COMMENT '权限说明',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                   ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_permission_code` (`permission_code`),
    KEY `idx_sys_permission_parent` (`parent_id`),
    KEY `idx_sys_permission_status` (`status`, `is_delete`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '系统权限表';

-- ----------------------------
-- 3. 角色权限关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role_permission`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id`       BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_permission_permission` (`permission_id`),
    CONSTRAINT `fk_role_permission_role`
        FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT `fk_role_permission_permission`
        FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`)
            ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '角色权限关联表';

-- ----------------------------
-- 4. 初始化角色
-- 与 UserRoleEnum 保持一致
-- ----------------------------
INSERT IGNORE INTO `sys_role`
    (`role_name`, `role_code`, `description`, `sort_order`, `status`)
VALUES
    ('普通用户', 'user', '仅允许查看公开或已授权的设计内容', 10, 1),
    ('设计师', 'designer', '创建设计稿、修改设计稿并查看设计内容', 20, 1),
    ('审核人', 'reviewer', '查看设计内容并执行设计审核', 30, 1),
    ('项目验收人', 'project_acceptor', '验收指定的设计项目并填写验收意见', 35, 1),
    ('项目经理', 'project_manager', '管理项目并查看设计内容', 40, 1),
    ('系统管理员', 'admin', '拥有系统全部管理权限', 50, 1);

-- ----------------------------
-- 5. 初始化当前后端已使用的权限
-- 权限编码与 StpInterfaceImpl 保持一致
-- ----------------------------
INSERT IGNORE INTO `sys_permission`
    (`permission_name`, `permission_code`, `permission_type`, `path`,
     `sort_order`, `status`, `description`)
VALUES
    ('查看设计', 'design:view', 2, NULL, 10, 1, '查看设计稿及相关信息'),
    ('创建设计', 'design:create', 2, NULL, 20, 1, '创建并上传设计稿'),
    ('修改设计', 'design:update', 2, NULL, 30, 1, '修改本人或已授权的设计稿'),
    ('审核设计', 'design:review', 2, NULL, 40, 1, '审核设计稿并填写审核意见'),
    ('项目管理', 'project:manage', 2, NULL, 50, 1, '创建、修改、分配和关闭项目'),
    ('用户管理', 'user:manage', 2, '/user/**', 60, 1, '新增、查询、修改和删除用户');

-- ----------------------------
-- 6. 初始化角色权限
-- INSERT IGNORE 使脚本可重复执行
-- ----------------------------

-- 普通用户：查看设计
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p ON p.permission_code IN ('design:view')
WHERE r.role_code = 'user';

-- 设计师：查看、创建、修改设计
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p
    ON p.permission_code IN ('design:view', 'design:create', 'design:update')
WHERE r.role_code = 'designer';

-- 审核人：查看、审核设计
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p
    ON p.permission_code IN ('design:view', 'design:review')
WHERE r.role_code = 'reviewer';

-- 项目经理：项目管理、查看设计
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p
    ON p.permission_code IN ('project:manage', 'design:view')
WHERE r.role_code = 'project_manager';

-- 管理员：全部权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p
WHERE r.role_code = 'admin'
  AND p.status = 1
  AND p.is_delete = 0;

-- ----------------------------
-- 7. 执行后检查
-- ----------------------------
SELECT
    r.role_code,
    r.role_name,
    GROUP_CONCAT(
        p.permission_code
        ORDER BY p.sort_order
        SEPARATOR ', '
    ) AS permissions
FROM `sys_role` r
LEFT JOIN `sys_role_permission` rp ON rp.role_id = r.id
LEFT JOIN `sys_permission` p
    ON p.id = rp.permission_id
   AND p.status = 1
   AND p.is_delete = 0
WHERE r.is_delete = 0
GROUP BY r.id, r.role_code, r.role_name
ORDER BY r.sort_order;

-- Sa-Token 查询某个用户角色的 SQL 示例：
-- SELECT r.role_code
-- FROM `user` u
-- JOIN `sys_role` r ON r.role_code = u.user_role
-- WHERE u.id = ?
--   AND u.user_status = 1
--   AND u.is_delete = 0
--   AND r.status = 1
--   AND r.is_delete = 0;

-- Sa-Token 查询某个用户权限的 SQL 示例：
-- SELECT DISTINCT p.permission_code
-- FROM `user` u
-- JOIN `sys_role` r ON r.role_code = u.user_role
-- JOIN `sys_role_permission` rp ON rp.role_id = r.id
-- JOIN `sys_permission` p ON p.id = rp.permission_id
-- WHERE u.id = ?
--   AND u.user_status = 1
--   AND u.is_delete = 0
--   AND r.status = 1
--   AND r.is_delete = 0
--   AND p.status = 1
--   AND p.is_delete = 0
-- ORDER BY p.sort_order;
