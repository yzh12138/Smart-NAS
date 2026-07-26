# Smart-NAS 自动备份指南

本文档详细说明如何使用 WebDAV 协议将手机照片自动备份到 Smart-NAS。

---

## 目录

1. [功能概述](#1-功能概述)
2. [WebDAV 服务器配置](#2-webdav-服务器配置)
3. [iOS 设备备份设置](#3-ios-设备备份设置)
4. [Android 设备备份设置](#4-android-设备备份设置)
5. [Windows/Mac 备份设置](#5-windowsmac-备份设置)
6. [进阶：自动后台同步](#6-进阶自动后台同步)
7. [故障排除](#7-故障排除)
8. [安全建议](#8-安全建议)
9. [技术架构](#9-技术架构)

---

## 1. 功能概述

Smart-NAS 提供 WebDAV 服务器，支持以下备份方式：

| 平台 | 方式 | 自动同步 | 需要安装 App |
|------|------|----------|-------------|
| iOS | 系统 Files App | 手动 | 否 |
| iOS | ThirdParty App | 自动 | 是（推荐 FolderSync） |
| Android | 系统文件管理器 | 手动 | 否 |
| Android | ThirdParty App | 自动 | 是（推荐 FolderSync） |
| Windows | 资源管理器 | 手动 | 否 |
| Mac | Finder | 手动 | 否 |

### WebDAV 地址

```
http://<服务器IP>:8081/webdav/
```

例如：`http://192.168.1.100:8081/webdav/`

### 认证方式

使用 Smart-NAS 的用户名和密码（Basic Auth）。

---

## 2. WebDAV 服务器配置

### 2.1 默认配置

在 `application.yml` 中：

```yaml
webdav:
  username: admin    # WebDAV 用户名（默认与系统用户相同）
  password: admin    # WebDAV 密码
```

### 2.2 自定义配置

修改 `application.yml` 后重启服务：

```yaml
webdav:
  username: myuser
  password: mypassword

photo:
  storage:
    base-path: /data/photos  # 照片存储路径
```

### 2.3 存储结构

WebDAV 挂载在照片存储目录下的 `webdav/` 子目录：

```
D:\test\photos\
├── 2024\           # 按年份存储的照片
│   ├── 01\
│   └── 02\
├── thumbnails\     # 缩略图
├── avatars\        # 头像
└── webdav\         # WebDAV 同步目录
    ├── Camera\     # 手机相机照片
    ├── Screenshots\ # 截图
    └── ...
```

---

## 3. iOS 设备备份设置

### 方式一：使用系统 Files App（手动同步）

1. 打开 **Files** App
2. 点击右上角 **...** → **连接服务器**
3. 输入服务器地址：`http://192.168.1.100:8081/webdav/`
4. 选择 **注册用户**
5. 输入用户名和密码
6. 点击 **连接**

**上传照片：**
1. 打开 **照片** App
2. 选择要备份的照片
3. 点击分享按钮 → **存储到文件**
4. 选择 WebDAV 服务器目录
5. 点击 **存储**

### 方式二：使用 FolderSync Pro（自动同步，推荐）

1. 从 App Store 下载 **FolderSync Pro**（付费，约 ¥30）
2. 打开 App → **添加账户** → **WebDAV**
3. 配置：
   - 账户名称：`Smart-NAS`
   - 服务器地址：`http://192.168.1.100:8081/webdav/`
   - 用户名：`admin`
   - 密码：`admin`
4. 点击 **测试连接** → 成功后保存
5. 创建同步文件夹对：
   - 远程文件夹：`/Camera`
   - 本地文件夹：`DCIM`（相册）
   - 同步类型：**仅上传（到服务器）**
6. 开启 **计划同步** → 选择 **每天** 或 **WiFi 连接时**
7. 启用 **后台同步**

### 方式三：使用 PhotoSync（自动同步，推荐）

1. 从 App Store 下载 **PhotoSync**
2. 打开 App → **设置** → **目标**
3. 选择 **WebDAV**
4. 输入服务器信息：
   - 地址：`192.168.1.100`
   - 端口：`8081`
   - 路径：`/webdav`
   - 用户名：`admin`
   - 密码：`admin`
5. 返回主界面 → 点击 **同步** → 选择 **自动同步**
6. 选择要同步的相册（如"相机胶卷"）

---

## 4. Android 设备备份设置

### 方式一：使用系统文件管理器（手动同步）

1. 打开 **文件管理器** App
2. 点击 **网络** 或 **连接**
3. 选择 **WebDAV** 或 **添加网络位置**
4. 输入服务器地址：`http://192.168.1.100:8081/webdav/`
5. 输入用户名和密码
6. 连接后可浏览和上传文件

### 方式二：使用 FolderSync（自动同步，推荐）

1. 从 Google Play 下载 **FolderSync**
2. 打开 App → **账户** → **添加账户** → **WebDAV**
3. 配置：
   - 名称：`Smart-NAS`
   - 服务器地址：`http://192.168.1.100:8081`
   - 端口：`8081`
   - 路径：`/webdav/`
   - 用户名：`admin`
   - 密码：`admin`
4. 测试连接成功后保存
5. 创建同步配对：
   - 远程文件夹：`/Camera`
   - 本地文件夹：`/storage/emulated/0/DCIM`
   - 同步类型：**仅上传**
6. 设置计划：**每天** 或 **仅 WiFi**
7. 开启 **后台同步**

### 方式三：使用 AutoSync（免费替代）

1. 从 Google Play 下载 **AutoSync for WebDAV**
2. 配置服务器信息（同上）
3. 设置同步文件夹和计划

---

## 5. Windows/Mac 备份设置

### Windows 资源管理器

1. 打开 **文件资源管理器**
2. 右键 **此电脑** → **添加一个网络位置**
3. 选择 **选择自定义网络位置**
4. 输入地址：`http://192.168.1.100:8081/webdav/`
5. 输入用户名和密码
6. 命名（如 "Smart-NAS"）
7. 完成

**批量上传：**
- 直接拖拽文件到 WebDAV 文件夹
- 或使用 `xcopy` 命令：
  ```cmd
  xcopy "C:\Users\Photos\*" "\\192.168.1.100@8081\webdav\Camera\" /s /e
  ```

### Mac Finder

1. 打开 **Finder**
2. 菜单栏 → **前往** → **连接服务器**（或按 `⌘K`）
3. 输入地址：`http://192.168.1.100:8081/webdav/`
4. 点击 **连接**
5. 输入用户名和密码

### Windows 命令行（批量备份脚本）

```batch
@echo off
REM Smart-NAS 备份脚本
SET WEBDAV_URL=http://192.168.1.100:8081/webdav
SET SOURCE_DIR=C:\Users\%USERNAME%\Pictures\Camera Roll
SET USERNAME=admin
SET PASSWORD=admin

REM 使用 curl 上传
for %%f in ("%SOURCE_DIR%\*.*") do (
    curl -u %USERNAME%:%PASSWORD% -T "%%f" "%WEBDAV_URL%/Camera/"
)

echo 备份完成！
pause
```

### Mac/Linux 命令行

```bash
#!/bin/bash
# Smart-NAS 备份脚本

WEBDAV_URL="http://192.168.1.100:8081/webdav"
SOURCE_DIR="$HOME/Pictures/Photos Library.photoslibrary/originals"
USERNAME="admin"
PASSWORD="admin"

# 使用 curl 上传
find "$SOURCE_DIR" -type f \( -name "*.jpg" -o -name "*.png" -o -name "*.heic" \) | while read file; do
    curl -u "$USERNAME:$PASSWORD" -T "$file" "$WEBDAV_URL/Camera/"
    echo "Uploaded: $file"
done

echo "备份完成！"
```

---

## 6. 进阶：自动后台同步

### 方案一：FolderSync 定时任务（推荐）

在 FolderSync 中设置：
- **同步计划**：每天凌晨 2:00
- **仅 WiFi**：开启（避免消耗流量）
- **仅充电时**：开启（可选）
- **后台同步**：开启

### 方案二：Tasker + FolderSync（Android 自动触发）

使用 Tasker 监听：
1. **新照片事件**：当 DCIM 目录有新文件时
2. **触发动作**：启动 FolderSync 同步

### 方案三：iOS 快捷指令自动化

1. 打开 **快捷指令** App
2. 创建 **自动化** → **特定时间** → 每天凌晨 2:00
3. 添加动作：**运行 FolderSync 同步**（需 FolderSync 支持快捷指令）

### 方案四：Syncthing 集成（双向同步）

如果需要双向同步（手机和 NAS 互相备份）：

1. 在 NAS 上安装 Syncthing：
   ```bash
   docker run -d --name syncthing \
     -p 8384:8384 -p 22000:22000 \
     -v /data/syncthing:/var/syncthing \
     syncthing/syncthing
   ```

2. 手机安装 Syncthing App
3. 配置同步文件夹对：
   - NAS: `/data/photos/webdav/Camera`
   - 手机: `/storage/emulated/0/DCIM`

---

## 7. 故障排除

### 连接失败

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 无法连接 | 服务器未启动 | 检查 service-business 是否运行在 8081 端口 |
| 连接超时 | 防火墙阻断 | 开放 8081 端口 |
| 认证失败 | 用户名/密码错误 | 使用 Smart-NAS 的用户名和密码 |
| 404 错误 | 路径错误 | 确保地址以 `/webdav/` 结尾（注意最后的斜杠） |

### 上传失败

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 上传 0 字节 | 文件被锁定 | 关闭其他正在使用该文件的程序 |
| 磁盘空间不足 | 存储目录已满 | 清理空间或修改 `photo.storage.base-path` |
| 文件名乱码 | 编码问题 | 确保使用 UTF-8 编码 |

### 同步不自动

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| FolderSync 不自动同步 | 省电模式限制 | 将 FolderSync 加入电池优化白名单 |
| iOS 后台被杀 | 系统限制 | iOS 后台同步有限，建议使用 PhotoSync |
| 同步延迟 | 计划设置不当 | 检查同步计划配置 |

### 日志查看

查看 WebDAV 请求日志：

```bash
# 查看服务日志
tail -f logs/service-business.log | grep -i webdav
```

---

## 8. 安全建议

### 8.1 修改默认密码

**重要：** 首次使用后请立即修改 WebDAV 密码！

```yaml
# application.yml
webdav:
  username: myuser
  password: your-strong-password-here
```

### 8.2 使用 HTTPS（生产环境）

在生产环境中，建议使用 HTTPS 加密传输：

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: your-keystore-password
    key-store-type: PKCS12
```

生成自签名证书：

```bash
keytool -genkeypair -alias smart-nas -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 \
  -storepass changeit -validity 365
```

### 8.3 限制访问 IP

如果只需要局域网访问，可以在 Gateway 层限制：

```yaml
# gateway 配置
spring:
  cloud:
    gateway:
      routes:
        - id: webdav
          uri: lb://service-business
          predicates:
            - Path=/webdav/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

### 8.4 定期备份

建议定期备份 WebDAV 目录：

```bash
# 每周备份 webdav 目录
0 3 * * 0 tar -czf /backup/webdav-$(date +\%Y\%m\%d).tar.gz D:/test/photos/webdav/
```

---

## 9. 技术架构

### 9.1 WebDAV 协议支持

Smart-NAS WebDAV 服务器支持以下方法：

| 方法 | 说明 | 用途 |
|------|------|------|
| `OPTIONS` | 查询支持的方法 | 客户端发现服务器能力 |
| `PROPFIND` | 查询属性 | 浏览目录结构 |
| `GET` | 下载文件 | 下载照片 |
| `PUT` | 上传文件 | 上传照片（核心） |
| `DELETE` | 删除文件 | 删除照片 |
| `MKCOL` | 创建目录 | 创建文件夹 |
| `MOVE` | 移动/重命名 | 整理文件 |
| `COPY` | 复制文件 | 备份文件 |

### 9.2 认证流程

```
客户端发起请求
    ↓
服务器返回 401 + WWW-Authenticate: Basic
    ↓
客户端发送 Authorization: Basic base64(user:pass)
    ↓
服务器验证用户名密码（BCrypt 哈希比较）
    ↓
验证通过 → 执行操作
验证失败 → 返回 401
```

### 9.3 存储映射

```
WebDAV 路径          →  本地文件系统路径
/webdav/             →  D:\test\photos\webdav\
/webdav/Camera/      →  D:\test\photos\webdav\Camera\
/webdav/photo.jpg    →  D:\test\photos\webdav\photo.jpg
```

### 9.4 与其他功能的集成

WebDAV 上传的照片**不会自动入库**到 Smart-NAS 数据库。如需入库：

1. **手动方式**：通过前端"上传照片"功能上传
2. **自动方式**（需开发）：添加文件监听器，自动扫描 `webdav/` 目录的新文件并入库

**未来计划：**
- 添加 `FileSystemWatcher` 监听 `webdav/` 目录变化
- 新文件自动调用 EXIF 解析和 AI 分析
- 自动生成缩略图并入库

---

## 附录：WebDAV 客户端推荐

| 平台 | App | 价格 | 自动同步 | 推荐度 |
|------|-----|------|----------|--------|
| iOS | PhotoSync | ¥30 | 是 | ★★★★★ |
| iOS | FolderSync Pro | ¥30 | 是 | ★★★★☆ |
| iOS | Files（系统） | 免费 | 否 | ★★★☆☆ |
| Android | FolderSync | 免费/Pro | 是 | ★★★★★ |
| Android | AutoSync | 免费 | 是 | ★★★★☆ |
| Windows | 资源管理器 | 免费 | 否 | ★★★☆☆ |
| Mac | Finder | 免费 | 否 | ★★★☆☆ |
| 命令行 | curl | 免费 | 脚本 | ★★★★☆ |

---

**最后更新：** 2026-07-25

**版本：** v1.2
