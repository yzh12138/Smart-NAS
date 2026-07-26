-- v13: 照片点击计数
ALTER TABLE `photo` ADD COLUMN `click_count` INT DEFAULT 0 COMMENT '点击/浏览次数' AFTER `recycle_days`;
