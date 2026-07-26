# MIMO Smart-NAS

基于 Java + Python + Vue 3 的本地智能照片管理系统。支持照片/视频上传与自动标签、EXIF GPS 解析、地图回忆浏览、AI 对话、人脸识别、城市照片管理、WebDAV 备份、家庭共享、图书管理、文件存储，以及完整的 RBAC 权限管理。通过本地 Ollama 运行 Qwen2.5 大模型实现 AI 图片分析和智能对话。

## 技术栈

| 层 | 技术 | 版本 | 用途 |
|---|---|---|---|
| 网关 | Spring Cloud Gateway | 2023.0.1 | 统一入口、JWT 鉴权、路由转发、跨域 |
| 注册中心 | Nacos | 2.3.1 | 微服务注册与发现 |
| 业务服务 | Spring Boot | 3.2.5 | 核心业务逻辑 |
| ORM | MyBatis-Plus | 3.5.6 | 数据库操作 |
| 数据库 | MySQL | 8.0 | 持久化存储 |
| 缓存 | Redis | 7 | JWT Token 缓存 |
| 密码加密 | BCrypt (jBCrypt) | - | 单向哈希密码存储（已从 AES 迁移） |
| AI 服务 | Python FastAPI | 0.111.0 | 缩略图生成、EXIF 解析、AI 图片分析、AI 对话 |
| 大模型 | Ollama + Qwen2.5 / Qwen2.5vl | 7b | 图片识别、标签生成、智能对话 |
| 前端 | Vue 3 + Vite + Element Plus + ECharts | 3.4.27 | 用户界面 |
| EXIF 解析 | metadata-extractor | 2.19.0 | 读取照片 GPS 坐标 |
| WebDAV | 自研 WebDAV Filter | - | 手机/电脑照片自动备份 |

## 项目结构

```
smart-nas/
├── gateway/                          # Spring Cloud Gateway 统一网关
│   └── src/main/java/yzh/stock/gateway/
│       ├── GatewayApplication.java
│       └── filter/AuthGlobalFilter.java
│
├── service-business/                 # 核心业务微服务
│   └── src/main/java/yzh/stock/business/
│       ├── controller/               # 19 个 REST 控制器
│       ├── service/                  # 18 个业务服务
│       ├── entity/                   # 20+ 个实体类
│       ├── mapper/                   # MyBatis Mapper
│       ├── utils/                    # 工具类（PasswordUtil 等）
│       └── config/                   # 配置类（WebDAV、数据初始化等）
│
├── service-ai-analyze/               # Python AI 服务
│   ├── main.py
│   └── app/
│       ├── routers/                  # thumbnail / exif / chat
│       └── services/                 # ai_client / ollama_client
│
├── web-vue/                          # Vue 3 前端
│   └── src/
│       ├── api/index.js              # 所有 API 接口
│       ├── components/Layout/        # 侧边栏 + 顶栏布局
│       └── views/                    # 20+ 个页面组件
│
├── middleware-compose/               # 中间件 Docker Compose
│   ├── docker-compose.yml
│   └── init-sql/
│       ├── init.sql                  # 初始化 SQL
│       └── migration-*.sql           # 数据库迁移脚本
│
└── pom.xml                           # Maven Parent POM
```

## 功能模块

### 1. Gateway（端口 8080）

Spring Cloud Gateway 统一网关，所有前端请求经过此处转发到后端服务。

**核心功能：**
- **路由转发**：`/api/auth/**`、`/api/system/**`、`/api/photo/**`、`/api/tag/**`、`/api/recycle/**`、`/api/family/**`、`/api/ai-model/**`、`/api/log/**`、`/api/duplicate/**`、`/api/file/**`、`/api/book/**`、`/api/ai-chat/**` → service-business；`/api/ai/**` → Python AI 服务
- **JWT 鉴权**：全局拦截，校验 Bearer Token，白名单 `/api/auth/login` 不拦截
- **跨域处理**：允许前端开发服务器跨域访问

### 2. service-business（端口 8081）

核心业务微服务，承载所有数据 CRUD 和业务逻辑。

| 模块 | Controller | 功能 |
|---|---|---|
| 认证 | AuthController | JWT 登录/登出、获取当前用户信息 |
| 用户管理 | SysUserController | 用户 CRUD、分页查询、状态启禁用 |
| 角色管理 | SysRoleController | 角色 CRUD、用户-角色关联 |
| 权限管理 | SysPermissionController | 三级权限树（目录→菜单→按钮） |
| 照片管理 | PhotoController | 多文件上传、EXIF GPS 解析、缩略图、城市统计、搜索、点击跟踪、推荐照片 |
| 视频管理 | （复用 PhotoController） | 视频上传、播放、删除、批量操作 |
| 标签管理 | TagController | 标签 CRUD、照片-标签关联（逗号分隔符） |
| 人脸识别 | FaceController | 人脸聚类、照片关联、移动/删除操作 |
| 城市管理 | CityController | 城市 CRUD、照片统计 |
| AI 对话 | AiChatController | 创建对话、发送消息、历史记录 |
| AI 模型 | AiModelController | 模型配置 CRUD、设置默认模型、全局提示词管理 |
| AI 提示词 | AiUserPromptController | 用户自定义提示词 CRUD、设为默认 |
| 家庭共享 | FamilyController | 创建家庭、成员管理、照片共享 |
| 好友管理 | UserFriendController | 好友搜索、添加、接受/拒绝请求 |
| 照片留言 | PhotoCommentController | 照片评论/留言 CRUD |
| 回收站 | RecycleController | 照片回收、恢复、永久删除、清空 |
| 文件存储 | FileStorageController | 任意文件上传/下载/删除 |
| 图书管理 | BookController | 电子书上传/阅读/管理 |
| 重复检测 | DuplicateController | 重复文件扫描与清理 |
| 操作日志 | LogController | 操作日志查询 |

### 3. service-ai-analyze（端口 8000）

Python FastAPI 服务，提供图片处理和 AI 能力。

| 接口 | 方法 | 功能 |
|---|---|---|
| `/api/ai/thumbnail` | POST | 生成缩略图（Pillow，300x300） |
| `/api/ai/parse-exif` | POST | 解析 EXIF 数据（GPS、拍摄时间） |
| `/api/ai/analyze-image` | POST | 调用 Ollama Qwen2.5vl 分析图片 |
| `/api/ai/chat` | POST | AI 对话（支持 Ollama 和 OpenAI 兼容 API） |
| `/health` | GET | 健康检查 |

### 4. web-vue（端口 5173）

Vue 3 前端应用，支持中英文双语。

**页面功能：**

| 页面 | 路由 | 功能 |
|---|---|---|
| 登录页 | `/login` | 猫狗主题背景、用户注册 |
| 仪表盘 | `/dashboard` | 统计卡片（可点击跳转）、最近照片、城市分布饼图 |
| 上传照片 | `/photo/upload` | 拖拽多文件上传、相似标签提示、缩略图预览、手动/AI 标签 |
| 照片回忆 | `/photo/memory` | ECharts 中国地图（市级）、城市筛选、照片网格 |
| 照片总览 | `/photo/overview` | 照片网格、标签筛选、批量选择、日期范围筛选、留言功能、全屏预览 |
| AI 审核 | `/photo/review` | AI 标签审核队列、经纬度显示、扫描进度（暂停/取消） |
| 人脸识别 | `/face` | 人脸聚类管理、关联照片、移动/删除操作 |
| 视频管理 | `/video` | 视频网格、上传、搜索、批量删除、在线播放、共享到家庭 |
| 家庭共享 | `/family` | 共享主页、通过邀请码加入、撤销共享 |
| 家庭管理 | `/family/manage` | 创建家庭、成员审批、邀请码管理 |
| 好友管理 | `/friend` | 好友搜索、添加、接受/拒绝请求 |
| 任务中心 | `/task` | 家庭申请审批、好友请求审批 |
| 文件存储 | `/file` | 任意文件上传/下载/删除、分类管理（中英文） |
| 图书管理 | `/book` | 电子书上传、在线阅读（EPUB/PDF） |
| AI 对话 | `/ai-chat` | 多轮对话、模型切换、提示词管理、记忆编辑 |
| 手机AI对话 | `/ai-chat/mobile` | 移动端 AI 聊天界面 |
| 标签管理 | `/tag` | 标签 CRUD、分页、关联照片数量、颜色显示 |
| 城市管理 | `/city` | 城市 CRUD、照片统计、点击查看城市照片 |
| 回收站 | `/recycle` | 照片恢复、永久删除、保留天数设置 |
| 个人资料 | `/profile` | 修改昵称、密码、上传头像 |
| 用户/角色管理 | `/system/users` | 用户 CRUD、角色分配、状态切换 |
| 权限管理 | `/system/permissions` | 三级权限树管理（仅管理员可见） |
| 系统设置 | `/system/settings` | 主题模式、语言切换、存储信息、AI 模型管理、重复检测（仅管理员） |
| 操作日志 | `/system/logs` | 操作日志查询（仅管理员可见） |

**侧边栏结构：**
```
📁 仪表盘
📁 照片管理
   ├── 上传照片
   ├── 照片回忆
   ├── 照片总览
   ├── AI 审核
   └── 人脸识别
📁 视频
📁 家庭共享
   ├── 共享主页
   ├── 家庭管理
   └── 好友管理
📁 文件存储
📁 图书管理
📁 AI 对话
📁 回收站
📁 任务中心
📁 系统管理
   ├── 个人资料
   ├── 用户管理
   ├── 角色管理
   ├── 权限管理（仅管理员）
   ├── 系统设置（仅管理员）
   ├── 操作日志（仅管理员）
   ├── 标签管理
   └── 城市管理
```

**悬浮助手：** 可拖拽的柴犬 AI 助手，点击打开 AI 对话，支持模型切换，可最小化到右侧半隐藏。

## 环境要求

| 组件 | 版本要求 | 说明 |
|---|---|---|
| JDK | 17+ | Java 编译和运行 |
| Maven | 3.8+ | 构建 Java 项目 |
| Node.js | 18+ | 前端开发和构建 |
| Python | 3.10+ | AI 服务运行 |
| Docker | 20+ | 运行中间件容器 |
| Ollama | latest | 本地大模型推理 |

## 快速启动

### 第一步：启动中间件

```bash
cd middleware-compose
docker-compose up -d
```

验证：
- MySQL：`mysql -h 127.0.0.1 -u root -proot`
- Redis：`redis-cli ping` → `PONG`
- RabbitMQ：`http://localhost:15672`（guest/guest）
- Nacos：`http://localhost:8848/nacos`（nacos/nacos）

### 第二步：安装 Ollama 并拉取模型

```bash
# 安装 Ollama（参考 https://ollama.com）

# 拉取视觉语言模型（图片分析）
ollama pull qwen2.5vl:7b

# 拉取文本模型（AI 对话）
ollama pull qwen2.5:7b
```

### 第三步：启动 Python AI 服务

```bash
cd service-ai-analyze
python -m venv venv
venv\Scripts\activate          # Windows
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

验证：`http://localhost:8000/health` → `{"status":"ok"}`

### 第四步：启动 Java 微服务

在 IntelliJ IDEA 中打开项目根目录：

1. 先启动 `GatewayApplication.java`（端口 8080）
2. 再启动 `BusinessApplication.java`（端口 8081）

验证 Nacos 注册：`http://localhost:8848/nacos` 服务列表中应出现两个服务。

### 第五步：启动前端

```bash
cd web-vue
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`，默认账号：`admin / admin`

## 配置说明

### 存储路径配置（service-business/src/main/resources/application.yml）

```yaml
photo:
  storage:
    base-path: D:\\test\\photos          # 照片原图存储路径
    thumbnail-path: D:\\test\\thumbnails  # 缩略图存储路径
    video-path: D:\\test\\videos          # 视频存储路径
```

### JWT 配置（gateway/src/main/resources/application.yml）

```yaml
jwt:
  secret: smart-nas-secret-key-must-be-at-least-32-bytes-long!!
  expiration: 86400000  # 24 小时
```

### AI 模型配置

默认模型通过数据库 `ai_model_config` 表配置，支持切换 Ollama 和 OpenAI 兼容 API。

## 数据库表结构

| 表名 | 说明 |
|---|---|
| `sys_user` | 用户表（含 ai_prompt 字段） |
| `sys_role` | 角色表 |
| `sys_permission` | 权限表（三级：目录/菜单/按钮） |
| `sys_user_role` | 用户-角色关联表 |
| `sys_role_permission` | 角色-权限关联表 |
| `photo` | 照片/视频表（含 mediaType、click_count、city） |
| `tag` | 标签表 |
| `photo_tag` | 照片-标签关联表 |
| `ai_conversation` | AI 对话会话表 |
| `ai_message` | AI 对话消息表 |
| `ai_model_config` | AI 模型配置表 |
| `ai_user_prompt` | AI 用户提示词表 |
| `family` | 家庭表 |
| `family_member` | 家庭成员表 |
| `family_media` | 家庭共享媒体表 |
| `file_storage` | 文件存储表 |
| `book` | 图书表 |
| `book_collection` | 图书收藏表 |
| `album` | 相册表 |
| `album_photo` | 相册-照片关联表 |
| `face_cluster` | 人脸聚类表 |
| `face_photo` | 人脸-照片关联表 |
| `city` | 城市表 |
| `scene_tag` | 场景标签表 |
| `operation_log` | 操作日志表 |
| `photo_comment` | 照片留言表 |
| `user_friend` | 用户好友关系表 |

## API 接口一览

### 认证模块

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录（BCrypt 密码验证） |
| GET | `/api/auth/info` | 获取当前用户信息 |
| POST | `/api/auth/logout` | 登出 |

### 照片/视频模块

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/photo/upload` | 上传照片/视频（支持多文件 + 标签 + AI 标签 + 城市） |
| GET | `/api/photo/list` | 分页查询（支持按标签、城市、日期范围筛选） |
| GET | `/api/photo/{id}` | 获取详情 |
| PUT | `/api/photo/{id}/name` | 修改名称 |
| DELETE | `/api/photo/{id}` | 删除（移入回收站） |
| GET | `/api/photo/{id}/thumb` | 获取缩略图 |
| GET | `/api/photo/{id}/original` | 获取原图/视频 |
| GET | `/api/photo/search` | 关键词搜索 |
| POST | `/api/photo/{id}/ai-tags` | AI 推荐标签 |
| POST | `/api/photo/{id}/confirm-tags` | 确认 AI 标签 |
| POST | `/api/photo/{id}/click` | 照片点击记录（推荐算法） |
| GET | `/api/photo/recommended` | 推荐照片列表 |
| GET | `/api/photo/map/cities` | 城市照片统计 |
| GET | `/api/photo/map/city/{city}` | 某城市照片列表 |

### 人脸识别模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/face/clusters` | 获取人脸聚类列表 |
| GET | `/api/face/cluster/{id}/photos` | 获取聚类照片 |
| POST | `/api/face/cluster` | 创建聚类 |
| PUT | `/api/face/cluster/{id}` | 重命名聚类 |
| DELETE | `/api/face/cluster/{id}` | 删除聚类 |
| DELETE | `/api/face/cluster/{clusterId}/photo/{photoId}` | 移除照片 |
| POST | `/api/face/move` | 移动照片到其他聚类 |

### 城市管理模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/city/list` | 获取城市列表 |
| GET | `/api/city/{id}` | 获取城市详情 |
| POST | `/api/city` | 创建城市 |
| PUT | `/api/city/{id}` | 更新城市 |
| DELETE | `/api/city/{id}` | 删除城市 |

### AI 对话模块

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/ai-chat/conversation` | 创建对话 |
| GET | `/api/ai-chat/conversations` | 获取对话列表 |
| GET | `/api/ai-chat/conversation/{id}/messages` | 获取消息历史 |
| POST | `/api/ai-chat/conversation/{id}/send` | 发送消息（支持图片） |
| DELETE | `/api/ai-chat/conversation/{id}` | 删除对话 |

### AI 提示词模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/ai-prompt/list` | 获取用户提示词列表 |
| POST | `/api/ai-prompt` | 创建提示词 |
| PUT | `/api/ai-prompt/{id}` | 更新提示词 |
| DELETE | `/api/ai-prompt/{id}` | 删除提示词 |
| POST | `/api/ai-prompt/{id}/default` | 设为默认提示词 |
| GET | `/api/ai-prompt/default` | 获取默认提示词 |

### 好友管理模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/friend/search` | 搜索用户 |
| POST | `/api/friend/request` | 发送好友请求 |
| GET | `/api/friend/requests` | 获取好友请求列表 |
| POST | `/api/friend/accept/{id}` | 接受好友请求 |
| POST | `/api/friend/reject/{id}` | 拒绝好友请求 |
| GET | `/api/friend/list` | 获取好友列表 |
| DELETE | `/api/friend/{id}` | 删除好友 |

### 照片留言模块

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/photo/{id}/comments` | 获取照片留言 |
| POST | `/api/photo/{id}/comment` | 添加留言 |
| DELETE | `/api/comment/{id}` | 删除留言 |

### AI 服务接口（Python，端口 8000）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/ai/chat` | AI 对话（Ollama / OpenAI 兼容） |
| POST | `/api/ai/thumbnail` | 生成缩略图 |
| POST | `/api/ai/parse-exif` | 解析 EXIF 数据 |
| POST | `/api/ai/analyze-image` | AI 图片分析 |

## 端口一览

| 服务 | 端口 | 管理界面 |
|---|---|---|
| Gateway | 8080 | - |
| Business Service | 8081 | - |
| Python AI Service | 8000 | http://localhost:8000/docs |
| MySQL | 3306 | - |
| Redis | 6379 | - |
| RabbitMQ | 5672 | http://localhost:15672 |
| Nacos | 8848 | http://localhost:8848/nacos |
| 前端 Dev Server | 5173 | http://localhost:5173 |

## 常见问题

### Q: AI 对话返回 HTTP 422 错误？
Java 17 的 `HttpClient` 默认使用 HTTP/2 h2c 升级，Python uvicorn 不支持 HTTP/2，导致请求体丢失。解决方法：在 `AiChatService.java` 中强制使用 HTTP/1.1：
```java
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .build();
```

### Q: 照片上传后没有缩略图？
检查 Python AI 服务是否正常运行，以及缩略图目录是否有写入权限。

### Q: AI 标签分析很慢？
Qwen2.5vl:7b 模型首次加载需要约 30 秒，后续分析单张图片约 5-15 秒。

### Q: Windows 路径问题？
配置文件中的路径使用 `\\` 双反斜杠（如 `D:\\test\\photos`），这是 Java YAML 配置的转义写法。

### Q: 登录密码验证失败？
系统已从 AES 加密迁移到 BCrypt 单向哈希。首次启动时 `DataInitializer` 会自动将旧密码迁移为 BCrypt 格式。如遇问题，检查数据库 `sys_user` 表中的密码字段是否以 `$2a$` 或 `$2b$` 开头。

### Q: WebDAV 无法连接？
WebDAV 服务内嵌在 service-business 中（端口 8081），访问地址为 `http://<IP>:8081/webdav/`。确保地址以 `/webdav/` 结尾（注意最后的斜杠）。

### Q: AI 提示词优先级是什么？
系统采用多层提示词优先级链：
1. 模型 promptTemplate（前缀）
2. 对话级记忆提示词
3. 用户级自定义提示词
4. 全局提示词（`ai_prompt.json`）
5. 默认硬编码提示词

## License

个人使用项目，未设置开源协议。
