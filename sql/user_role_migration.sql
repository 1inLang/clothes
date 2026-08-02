-- 用户角色关联表迁移脚本（MySQL 8.x）
-- 执行前请先备份数据库；本脚本应在旧 user.user_role 字段仍存在时执行一次。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_role`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT          NOT NULL COMMENT '用户ID',
    `role_id`     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_role` (`role_id`),
    CONSTRAINT `fk_user_role_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT `fk_user_role_role`
        FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
            ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户角色关联表';

-- 将旧字段中的角色编码迁移为关联记录。
INSERT IGNORE INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `sys_role` r ON r.role_code = u.user_role
WHERE u.is_delete = 0 AND r.is_delete = 0;

-- 旧角色为空或无效的用户统一补充普通用户角色，避免迁移后没有任何角色。
INSERT IGNORE INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `sys_role` r ON r.role_code = 'user' AND r.is_delete = 0
WHERE u.is_delete = 0
  AND NOT EXISTS (SELECT 1 FROM `user_role` ur WHERE ur.user_id = u.id);

ALTER TABLE `user` DROP COLUMN `user_role`;

SELECT u.id, u.user_account, GROUP_CONCAT(r.role_code ORDER BY r.sort_order, r.id) AS roles
FROM `user` u
LEFT JOIN `user_role` ur ON ur.user_id = u.id
LEFT JOIN `sys_role` r ON r.id = ur.role_id
WHERE u.is_delete = 0
GROUP BY u.id, u.user_account
ORDER BY u.id;
