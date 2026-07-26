-- Smart-NAS V2 迁移脚本
USE smart_nas;

-- ============================================
-- #3 标签体系重构：tag 表增加分类字段
-- ============================================
ALTER TABLE tag ADD COLUMN tag_category VARCHAR(50) DEFAULT 'other'
    COMMENT '标签分类: province/city/landscape/scene/food/people/other' AFTER tag_color;
ALTER TABLE tag ADD COLUMN tag_parent VARCHAR(50) DEFAULT NULL
    COMMENT '上级分类值' AFTER tag_category;

-- 预置标签分类枚举说明：
-- province: 省/直辖市/自治区（如：内蒙古、黑龙江）
-- city: 城市（如：呼和浩特、哈尔滨）
-- landscape: 风景（如：草原、山脉、沙漠、湖泊）
-- scene: 场景（如：建筑、街道、室内、夜景）
-- food: 美食
-- people: 人物
-- other: 其他

-- ============================================
-- #6 视频支持：photo 表增加视频相关字段
-- ============================================
ALTER TABLE photo ADD COLUMN duration INT DEFAULT NULL COMMENT '视频时长(秒)' AFTER ai_analyzed;
ALTER TABLE photo ADD COLUMN media_type VARCHAR(20) DEFAULT 'image' COMMENT '媒体类型: image/video' AFTER duration;
