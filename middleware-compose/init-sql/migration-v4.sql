-- Smart-NAS V4 迁移脚本
USE smart_nas;

-- ===== 重复文件检测 =====
ALTER TABLE photo ADD COLUMN file_hash VARCHAR(64) DEFAULT NULL COMMENT '文件SHA256哈希' AFTER file_size;
CREATE INDEX idx_file_hash ON photo(file_hash);

-- ===== 用户年龄分组（图书权限） =====
ALTER TABLE sys_user ADD COLUMN age_group VARCHAR(20) DEFAULT 'adult' COMMENT 'child/teen/adult' AFTER backup_path;

-- ===== 文件存储 =====
CREATE TABLE IF NOT EXISTS file_storage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50) COMMENT '文件扩展名',
    category VARCHAR(50) DEFAULT 'other' COMMENT 'installer/archive/document/other',
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- ===== 图书管理 =====
CREATE TABLE IF NOT EXISTS book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    isbn VARCHAR(20),
    category VARCHAR(50) DEFAULT 'other' COMMENT 'novel/textbook/reference/comic/other',
    tags VARCHAR(500) COMMENT '逗号分隔标签',
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_format VARCHAR(20) COMMENT 'epub/pdf/mobi/txt',
    visibility VARCHAR(20) DEFAULT 'private' COMMENT 'public/private',
    cover_path VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS book_collection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_book (user_id, book_id),
    FOREIGN KEY (book_id) REFERENCES book(id)
);

-- ===== AI 对话 =====
CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    model_config_id BIGINT,
    system_prompt TEXT,
    workspace_path VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    content TEXT NOT NULL,
    tokens INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id)
);

-- ===== 人脸聚类 =====
CREATE TABLE IF NOT EXISTS face_cluster (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    cluster_name VARCHAR(100),
    photo_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS face_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cluster_id BIGINT NOT NULL,
    photo_id BIGINT NOT NULL,
    face_bbox VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cluster_id) REFERENCES face_cluster(id),
    FOREIGN KEY (photo_id) REFERENCES photo(id)
);

-- ===== 场景标签 =====
CREATE TABLE IF NOT EXISTS scene_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    photo_id BIGINT NOT NULL,
    scene_type VARCHAR(50) NOT NULL COMMENT 'gathering/travel/id/receipt/other',
    scene_name VARCHAR(100),
    confidence DECIMAL(3,2),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (photo_id) REFERENCES photo(id)
);

-- ===== 智能相册 =====
CREATE TABLE IF NOT EXISTS album (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    album_name VARCHAR(200) NOT NULL,
    description TEXT,
    cover_photo_id BIGINT,
    auto_generated TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS album_photo (
    album_id BIGINT NOT NULL,
    photo_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (album_id, photo_id)
);
