-- ============================================================
-- Smart-NAS 完整数据库初始化脚本
-- 合并自 init.sql + migration-v2 ~ v13 全部迁移脚本
-- 执行方式: mysql -u root -proot < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_nas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_nas;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    ai_prompt TEXT COMMENT '用户级AI提示词',
    family_role VARCHAR(20) COMMENT '家庭角色: 爸爸/妈妈/哥哥/姐姐等',
    backup_path VARCHAR(500) COMMENT '备份存储路径',
    age_group VARCHAR(20) DEFAULT 'adult' COMMENT 'child/teen/adult',
    status TINYINT DEFAULT 1 COMMENT '0=待审批 1=正常 2=禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. 角色表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    role_key VARCHAR(50) UNIQUE NOT NULL,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. 权限表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT 0,
    perm_name VARCHAR(50) NOT NULL,
    perm_key VARCHAR(100) UNIQUE NOT NULL,
    perm_type TINYINT NOT NULL COMMENT '1=目录 2=菜单 3=按钮',
    path VARCHAR(255),
    component VARCHAR(255),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 4. 用户-角色关联
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- 5. 角色-权限关联
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================
-- 6. 照片/视频表
-- ============================================================
CREATE TABLE IF NOT EXISTS photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    file_size BIGINT,
    file_hash VARCHAR(64) COMMENT 'SHA256哈希(重复检测)',
    mime_type VARCHAR(50),
    width INT,
    height INT,
    gps_lat DECIMAL(10,7),
    gps_lng DECIMAL(10,7),
    city VARCHAR(100),
    province VARCHAR(100),
    shoot_time DATETIME,
    ai_analyzed TINYINT DEFAULT 0 COMMENT '0=未分析 1=已分析 2=已审核 3=已拒绝',
    duration INT DEFAULT 0 COMMENT '视频时长(秒)',
    media_type VARCHAR(20) DEFAULT 'image' COMMENT 'image/video',
    is_deleted TINYINT DEFAULT 0 COMMENT '0=正常 1=回收站',
    deleted_time DATETIME COMMENT '移入回收站时间',
    recycle_days INT DEFAULT 30 COMMENT '回收站保留天数',
    click_count INT DEFAULT 0 COMMENT '点击/浏览次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- ============================================================
-- 7. 标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(50) UNIQUE NOT NULL,
    tag_color VARCHAR(20),
    tag_category VARCHAR(50) DEFAULT 'other' COMMENT 'province/city/landscape/scene/food/people/other',
    tag_parent VARCHAR(50) COMMENT '上级分类值',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 8. 照片-标签关联
-- ============================================================
CREATE TABLE IF NOT EXISTS photo_tag (
    photo_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    tag_source TINYINT DEFAULT 1 COMMENT '1=用户手动 2=AI生成',
    PRIMARY KEY (photo_id, tag_id),
    FOREIGN KEY (photo_id) REFERENCES photo(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
);

-- ============================================================
-- 9. 照片留言表
-- ============================================================
CREATE TABLE IF NOT EXISTS photo_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    photo_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL COMMENT '留言内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_photo_id (photo_id),
    INDEX idx_user_id (user_id)
);

-- ============================================================
-- 10. 家庭表
-- ============================================================
CREATE TABLE IF NOT EXISTS family (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL COMMENT '创建者(管理员)',
    description VARCHAR(500),
    family_code VARCHAR(10) UNIQUE COMMENT '家庭邀请码',
    status TINYINT DEFAULT 1 COMMENT '1=正常 0=已解散',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES sys_user(id)
);

-- ============================================================
-- 11. 家庭成员表
-- ============================================================
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

-- ============================================================
-- 12. 家庭共享媒体表
-- ============================================================
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

-- ============================================================
-- 13. 好友关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS user_friend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '发起者',
    friend_id BIGINT NOT NULL COMMENT '被添加者',
    status TINYINT DEFAULT 0 COMMENT '0=待确认 1=已接受 2=已拒绝',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_friend_id (friend_id)
);

-- ============================================================
-- 14. AI 对话会话表
-- ============================================================
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

-- ============================================================
-- 15. AI 对话消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    content TEXT NOT NULL,
    image_path VARCHAR(500) COMMENT '图片路径',
    tokens INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id)
);

-- ============================================================
-- 16. AI 模型配置表
-- ============================================================
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

-- ============================================================
-- 17. AI 用户提示词表
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_user_prompt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '提示词名称',
    content TEXT COMMENT '提示词内容',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认 0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
);

-- ============================================================
-- 18. 文件存储表
-- ============================================================
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

-- ============================================================
-- 19. 图书表
-- ============================================================
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

-- ============================================================
-- 20. 图书收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS book_collection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_book (user_id, book_id),
    FOREIGN KEY (book_id) REFERENCES book(id)
);

-- ============================================================
-- 21. 城市管理表
-- ============================================================
CREATE TABLE IF NOT EXISTS city (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '城市名称',
    description VARCHAR(500) COMMENT '城市描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_city_name (name)
);

-- 初始化中国主要城市数据
INSERT IGNORE INTO city (name, description) VALUES
('北京', '中华人民共和国首都，政治、文化中心'),
('上海', '中国最大的经济中心城市，国际金融中心'),
('广州', '广东省省会，华南地区政治、经济、文化中心'),
('深圳', '中国经济特区，科技创新中心'),
('成都', '四川省省会，西南地区科技、商贸、金融中心'),
('杭州', '浙江省省会，电子商务中心'),
('武汉', '湖北省省会，华中地区中心城市'),
('南京', '江苏省省会，历史文化名城'),
('重庆', '直辖市，西南地区经济中心'),
('西安', '陕西省省会，历史文化名城'),
('天津', '直辖市，北方经济中心'),
('苏州', '江苏省经济中心，园林之城'),
('长沙', '湖南省省会，中部地区中心城市'),
('沈阳', '辽宁省省会，东北地区中心城市'),
('青岛', '山东省经济中心，海滨城市'),
('郑州', '河南省省会，交通枢纽城市'),
('大连', '辽宁省经济中心，港口城市'),
('东莞', '广东省制造业中心'),
('宁波', '浙江省经济中心，港口城市'),
('厦门', '福建省经济中心，海滨城市'),
('福州', '福建省省会，历史文化名城'),
('昆明', '云南省省会，春城'),
('合肥', '安徽省省会，科教城市'),
('哈尔滨', '黑龙江省省会，冰城'),
('济南', '山东省省会，泉城'),
('佛山', '广东省制造业重镇'),
('长春', '吉林省省会，汽车城'),
('温州', '浙江省经济中心'),
('石家庄', '河北省省会'),
('南宁', '广西壮族自治区首府'),
('贵阳', '贵州省省会，林城'),
('南昌', '江西省省会'),
('太原', '山西省省会'),
('乌鲁木齐', '新疆维吾尔自治区首府'),
('兰州', '甘肃省省会'),
('海口', '海南省省会，椰城'),
('呼和浩特', '内蒙古自治区首府'),
('银川', '宁夏回族自治区首府'),
('西宁', '青海省省会'),
('拉萨', '西藏自治区首府，日光城'),
('香港', '国际金融中心，购物天堂'),
('澳门', '世界旅游休闲中心'),
('台北', '台湾省会'),
('高雄', '台湾南部经济中心'),
('东京', '日本首都，国际大都市'),
('大阪', '日本第二大城市'),
('首尔', '韩国首都'),
('釜山', '韩国第二大城市'),
('曼谷', '泰国首都'),
('新加坡', '东南亚金融中心'),
('吉隆坡', '马来西亚首都'),
('雅加达', '印度尼西亚首都'),
('河内', '越南首都'),
('胡志明市', '越南最大城市'),
('马尼拉', '菲律宾首都'),
('仰光', '缅甸最大城市'),
('金边', '柬埔寨首都'),
('万象', '老挝首都'),
('纽约', '美国最大城市，国际金融中心'),
('洛杉矶', '美国第二大城市，好莱坞所在地'),
('旧金山', '美国科技中心，硅谷所在地'),
('芝加哥', '美国第三大城市'),
('休斯顿', '美国第四大城市，航天中心'),
('西雅图', '美国科技中心，微软亚马逊总部'),
('波士顿', '美国教育中心，哈佛MIT所在地'),
('华盛顿', '美国首都'),
('迈阿密', '美国度假胜地'),
('拉斯维加斯', '世界娱乐之都'),
('伦敦', '英国首都，国际金融中心'),
('巴黎', '法国首都，浪漫之都'),
('柏林', '德国首都'),
('慕尼黑', '德国南部经济中心'),
('法兰克福', '欧洲金融中心'),
('罗马', '意大利首都，永恒之城'),
('马德里', '西班牙首都'),
('巴塞罗那', '西班牙第二大城市'),
('阿姆斯特丹', '荷兰首都'),
('布鲁塞尔', '比利时首都，欧盟总部'),
('维也纳', '奥地利首都，音乐之都'),
('苏黎世', '瑞士金融中心'),
('日内瓦', '国际组织总部所在地'),
('斯德哥尔摩', '瑞典首都'),
('哥本哈根', '丹麦首都'),
('赫尔辛基', '芬兰首都'),
('华沙', '波兰首都'),
('布拉格', '捷克首都'),
('布达佩斯', '匈牙利首都'),
('雅典', '希腊首都'),
('里斯本', '葡萄牙首都'),
('都柏林', '爱尔兰首都'),
('莫斯科', '俄罗斯首都'),
('圣彼得堡', '俄罗斯第二大城市'),
('悉尼', '澳大利亚最大城市'),
('墨尔本', '澳大利亚第二大城市'),
('布里斯班', '澳大利亚第三大城市'),
('珀斯', '澳大利亚西部中心城市'),
('奥克兰', '新西兰最大城市'),
('迪拜', '阿联酋经济中心'),
('阿布扎比', '阿联酋首都'),
('多哈', '卡塔尔首都'),
('利雅得', '沙特阿拉伯首都'),
('伊斯坦布尔', '土耳其最大城市'),
('开罗', '埃及首都'),
('约翰内斯堡', '南非最大城市'),
('内罗毕', '肯尼亚首都'),
('墨西哥城', '墨西哥首都'),
('圣保罗', '巴西最大城市'),
('里约热内卢', '巴西第二大城市'),
('布宜诺斯艾利斯', '阿根廷首都'),
('圣地亚哥', '智利首都'),
('利马', '秘鲁首都'),
('波哥大', '哥伦比亚首都'),
('多伦多', '加拿大最大城市'),
('温哥华', '加拿大西部中心城市'),
('蒙特利尔', '加拿大第二大城市');

-- ============================================================
-- 22. 人脸聚类表
-- ============================================================
CREATE TABLE IF NOT EXISTS face_cluster (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    cluster_name VARCHAR(100),
    photo_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- ============================================================
-- 23. 人脸照片关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS face_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cluster_id BIGINT NOT NULL,
    photo_id BIGINT NOT NULL,
    face_bbox VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cluster_id) REFERENCES face_cluster(id),
    FOREIGN KEY (photo_id) REFERENCES photo(id)
);

-- ============================================================
-- 24. 智能相册表
-- ============================================================
CREATE TABLE IF NOT EXISTS album (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    album_name VARCHAR(200) NOT NULL,
    description TEXT,
    cover_photo_id BIGINT,
    auto_generated TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 25. 相册-照片关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS album_photo (
    album_id BIGINT NOT NULL,
    photo_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (album_id, photo_id)
);

-- ============================================================
-- 26. 场景标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS scene_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    photo_id BIGINT NOT NULL,
    scene_type VARCHAR(50) NOT NULL COMMENT 'gathering/travel/id/receipt/other',
    scene_name VARCHAR(100),
    confidence DECIMAL(3,2),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (photo_id) REFERENCES photo(id)
);

-- ============================================================
-- 27. 操作日志表
-- ============================================================
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

-- ============================================================
-- 性能索引
-- ============================================================
CREATE INDEX idx_photo_user_deleted ON photo(user_id, is_deleted);
CREATE INDEX idx_photo_user_media ON photo(user_id, media_type, is_deleted);
CREATE INDEX idx_photo_city ON photo(city, is_deleted);
CREATE INDEX idx_photo_province ON photo(province, is_deleted);
CREATE INDEX idx_photo_file_hash ON photo(file_hash);
CREATE INDEX idx_photo_shoot_time ON photo(shoot_time);
CREATE INDEX idx_photo_original_name ON photo(original_name);
CREATE INDEX idx_photo_tag_photo ON photo_tag(photo_id);
CREATE INDEX idx_photo_tag_tag ON photo_tag(tag_id);
CREATE INDEX idx_ai_message_conv ON ai_message(conversation_id, create_time);
CREATE INDEX idx_ai_conversation_user ON ai_conversation(user_id, update_time);
CREATE INDEX idx_family_media_family ON family_media(family_id);
CREATE INDEX idx_family_member_user ON family_member(user_id, status);
CREATE INDEX idx_face_photo_cluster ON face_photo(cluster_id);
CREATE INDEX idx_face_photo_photo ON face_photo(photo_id);
CREATE INDEX idx_file_storage_user ON file_storage(user_id);
CREATE INDEX idx_book_user ON book(user_id, visibility);
CREATE INDEX idx_scene_tag_photo ON scene_tag(photo_id);

-- ============================================================
-- 初始数据
-- ============================================================

-- 超级管理员（密码为明文，首次启动时 DataInitializer 会自动迁移到 BCrypt）
INSERT INTO sys_user (username, password, nickname) VALUES ('admin', 'admin', '超级管理员')
ON DUPLICATE KEY UPDATE username=username;

-- 初始角色
INSERT INTO sys_role (role_name, role_key, sort_order) VALUES ('超级管理员', 'admin', 1)
ON DUPLICATE KEY UPDATE role_name=role_name;
INSERT INTO sys_role (role_name, role_key, sort_order) VALUES ('普通用户', 'user', 2)
ON DUPLICATE KEY UPDATE role_name=role_name;

-- 关联 admin 用户和超级管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'admin' AND r.role_key = 'admin'
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 初始权限（目录-菜单-按钮三级）
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (1, 0, '系统管理', 'system', 1, '/system', NULL, 'Setting', 1)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (2, 1, '仪表盘', 'system:dashboard', 2, '/dashboard', 'views/dashboard/index', 'DataAnalysis', 1)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (3, 1, '用户管理', 'system:user', 2, '/system/users', 'views/system/Users', 'User', 2)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (4, 1, '角色管理', 'system:role', 2, '/system/roles', 'views/system/Roles', 'UserFilled', 3)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (5, 1, '权限管理', 'system:permission', 2, '/system/permissions', 'views/system/Permissions', 'Lock', 4)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (6, 0, '照片管理', 'photo', 1, '/photo', NULL, 'Picture', 2)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (7, 6, '上传照片', 'photo:upload', 2, '/photo/upload', 'views/photo/Upload', 'Upload', 1)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (8, 6, '照片回忆', 'photo:memory', 2, '/photo/memory', 'views/photo/Memory', 'Location', 2)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

-- 超级管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.role_key = 'admin'
ON DUPLICATE KEY UPDATE role_id=role_id;

-- 预置 AI 模型配置
INSERT INTO ai_model_config (model_name, model_type, model_id, api_url, prompt_template, is_default)
VALUES ('Qwen2.5vl (本地)', 'ollama', 'qwen2.5vl:7b', 'http://localhost:11434',
'请分析这张照片，返回 JSON 格式的结果：\n{"tags": ["标签1", "标签2"], "city": "城市名", "province": "省份名", "description": "一句话描述"}\n注意：tags 是照片相关标签，3-5个；无法识别地点则 city/province 设为 null；只返回 JSON。',
1);
