-- v12: 用户家庭角色
ALTER TABLE `sys_user` ADD COLUMN `family_role` VARCHAR(20) DEFAULT NULL COMMENT '家庭角色' AFTER `avatar`;
