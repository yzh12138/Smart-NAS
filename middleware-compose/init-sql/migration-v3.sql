-- Smart-NAS V3 迁移脚本
USE smart_nas;

-- ===== 回收站 =====
ALTER TABLE photo ADD COLUMN is_deleted TINYINT DEFAULT 0 COMMENT '0=正常 1=回收站' AFTER media_type;
ALTER TABLE photo ADD COLUMN deleted_time DATETIME DEFAULT NULL COMMENT '移入回收站时间' AFTER is_deleted;
ALTER TABLE photo ADD COLUMN recycle_days INT DEFAULT 30 COMMENT '回收站保留天数' AFTER deleted_time;

-- ===== 家庭共享 =====
CREATE TABLE IF NOT EXISTS family (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL COMMENT '创建者(管理员)',
    description VARCHAR(500),
    status TINYINT DEFAULT 1 COMMENT '1=正常 0=已解散',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS family_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) DEFAULT 'member' COMMENT 'admin/member',
    status TINYINT DEFAULT 0 COMMENT '0=待审批 1=已通过 2=已拒绝',
    join_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_family_user (family_id, user_id),
    FOREIGN KEY (family_id) REFERENCES family(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS family_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    photo_id BIGINT NOT NULL,
    shared_by BIGINT NOT NULL COMMENT '分享者',
    share_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES family(id),
    FOREIGN KEY (photo_id) REFERENCES photo(id),
    FOREIGN KEY (shared_by) REFERENCES sys_user(id)
);

-- ===== AI 模型配置 =====
CREATE TABLE IF NOT EXISTS ai_model_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    model_type VARCHAR(50) NOT NULL COMMENT 'ollama/openai/mimo/other',
    model_id VARCHAR(200) COMMENT '模型ID',
    api_url VARCHAR(500) COMMENT 'API地址',
    api_key VARCHAR(500) COMMENT 'API密钥',
    prompt_template TEXT COMMENT '提示词模板',
    is_default TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ===== 操作日志 =====
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '操作对象类型',
    target_id BIGINT COMMENT '操作对象ID',
    detail TEXT COMMENT '操作详情',
    ip_address VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
);

-- ===== 备份配置 =====
ALTER TABLE sys_user ADD COLUMN backup_path VARCHAR(500) DEFAULT NULL COMMENT '备份存储路径(管理员设置)';

-- ===== 预置 AI 模型配置 =====
INSERT INTO ai_model_config (model_name, model_type, model_id, api_url, prompt_template, is_default)
VALUES ('Qwen2.5vl (本地)', 'ollama', 'qwen2.5vl:7b', 'http://localhost:11434',
'请分析这张照片，返回 JSON 格式的结果：\n{"tags": ["标签1", "标签2"], "city": "城市名", "province": "省份名", "description": "一句话描述"}\n注意：tags 是照片相关标签，3-5个；无法识别地点则 city/province 设为 null；只返回 JSON。',
1);
