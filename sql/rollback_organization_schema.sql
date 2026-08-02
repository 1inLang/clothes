-- 企业与部门模块数据库回滚脚本
-- 警告：执行后 sys_company、sys_department 中的数据不可恢复。
-- 请确认不再需要企业与部门数据并完成备份后再执行。

SET NAMES utf8mb4;

START TRANSACTION;

DELETE rp
FROM `sys_role_permission` rp
INNER JOIN `sys_permission` p ON p.id = rp.permission_id
WHERE p.permission_code IN ('org:view', 'org:manage');

DELETE FROM `sys_permission`
WHERE permission_code IN ('org:view', 'org:manage');

COMMIT;

DROP TABLE IF EXISTS `sys_department`;
DROP TABLE IF EXISTS `sys_company`;

ALTER TABLE `user`
    DROP INDEX `idx_user_company`,
    DROP INDEX `idx_user_department`,
    DROP COLUMN `department_id`,
    DROP COLUMN `company_id`;
