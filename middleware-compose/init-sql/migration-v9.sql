-- 家庭邀请码
ALTER TABLE `family` ADD COLUMN `family_code` varchar(10) DEFAULT NULL COMMENT '家庭邀请码' AFTER `description`;
CREATE UNIQUE INDEX `uk_family_code` ON `family` (`family_code`);
