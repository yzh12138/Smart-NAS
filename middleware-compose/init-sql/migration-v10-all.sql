-- =============================================
-- Smart-NAS 数据库迁移汇总 (v6 ~ v10)
-- 执行方式: mysql -u root -proot smart_nas < migration-v10-all.sql
-- =============================================

-- v6: AI用户提示词表
CREATE TABLE IF NOT EXISTS `ai_user_prompt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL COMMENT '提示词名称',
  `content` text COMMENT '提示词内容',
  `is_default` tinyint DEFAULT 0 COMMENT '是否默认 0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI用户提示词';

-- v7: 照片留言表
CREATE TABLE IF NOT EXISTS `photo_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `photo_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL COMMENT '留言内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_photo_id` (`photo_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照片留言';

-- v8: 用户好友关系表
CREATE TABLE IF NOT EXISTS `user_friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '发起者',
  `friend_id` bigint NOT NULL COMMENT '被添加者',
  `status` tinyint DEFAULT 0 COMMENT '0=待确认 1=已接受 2=已拒绝',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户好友关系';

-- v9: 家庭邀请码
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'family' AND COLUMN_NAME = 'family_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `family` ADD COLUMN `family_code` varchar(10) DEFAULT NULL COMMENT ''家庭邀请码'' AFTER `description`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'family' AND INDEX_NAME = 'uk_family_code');
SET @sql2 = IF(@idx_exists = 0, 'CREATE UNIQUE INDEX `uk_family_code` ON `family` (`family_code`)', 'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
