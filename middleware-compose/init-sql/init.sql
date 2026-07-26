-- Smart-NAS 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS smart_nas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_nas;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    family_role VARCHAR(20),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    role_key VARCHAR(50) UNIQUE NOT NULL,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 权限表
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

-- 用户-角色关联
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 角色-权限关联
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

-- 照片表
CREATE TABLE IF NOT EXISTS photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(50),
    width INT,
    height INT,
    gps_lat DECIMAL(10,7),
    gps_lng DECIMAL(10,7),
    city VARCHAR(100),
    province VARCHAR(100),
    shoot_time DATETIME,
    ai_analyzed TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(50) UNIQUE NOT NULL,
    tag_color VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 照片-标签关联
CREATE TABLE IF NOT EXISTS photo_tag (
    photo_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    tag_source TINYINT DEFAULT 1 COMMENT '1=用户手动 2=AI生成',
    PRIMARY KEY (photo_id, tag_id)
);

-- 初始数据：超级管理员（密码为明文，首次启动时会自动加密）
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
-- 系统管理目录
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (1, 0, '系统管理', 'system', 1, '/system', NULL, 'Setting', 1)
ON DUPLICATE KEY UPDATE perm_name=perm_name;
-- 仪表盘菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (2, 1, '仪表盘', 'system:dashboard', 2, '/dashboard', 'views/dashboard/index', 'DataAnalysis', 1)
ON DUPLICATE KEY UPDATE perm_name=perm_name;
-- 用户管理菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (3, 1, '用户管理', 'system:user', 2, '/system/users', 'views/system/Users', 'User', 2)
ON DUPLICATE KEY UPDATE perm_name=perm_name;
-- 角色管理菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (4, 1, '角色管理', 'system:role', 2, '/system/roles', 'views/system/Roles', 'UserFilled', 3)
ON DUPLICATE KEY UPDATE perm_name=perm_name;
-- 权限管理菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (5, 1, '权限管理', 'system:permission', 2, '/system/permissions', 'views/system/Permissions', 'Lock', 4)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

-- 照片管理目录
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (6, 0, '照片管理', 'photo', 1, '/photo', NULL, 'Picture', 2)
ON DUPLICATE KEY UPDATE perm_name=perm_name;
-- 上传照片菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (7, 6, '上传照片', 'photo:upload', 2, '/photo/upload', 'views/photo/Upload', 'Upload', 1)
ON DUPLICATE KEY UPDATE perm_name=perm_name;
-- 照片回忆菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_key, perm_type, path, component, icon, sort_order)
VALUES (8, 6, '照片回忆', 'photo:memory', 2, '/photo/memory', 'views/photo/Memory', 'Location', 2)
ON DUPLICATE KEY UPDATE perm_name=perm_name;

-- 超级管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.role_key = 'admin'
ON DUPLICATE KEY UPDATE role_id=role_id;

-- 人脸聚类表
CREATE TABLE IF NOT EXISTS face_cluster (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    cluster_name VARCHAR(100),
    photo_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 人脸照片关联表
CREATE TABLE IF NOT EXISTS face_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cluster_id BIGINT NOT NULL,
    photo_id BIGINT NOT NULL,
    face_bbox VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cluster_id) REFERENCES face_cluster(id),
    FOREIGN KEY (photo_id) REFERENCES photo(id)
);
