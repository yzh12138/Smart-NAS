# Smart NAS Android 客户端

基于 Smart NAS 后端 API 的 Android 原生客户端，使用 Kotlin + Jetpack Compose + Material 3 构建。

## 技术栈

| 技术 | 用途 |
|---|---|
| Kotlin | 开发语言 |
| Jetpack Compose | 声明式 UI 框架 |
| Material 3 (Material You) | 设计系统 |
| Hilt | 依赖注入 |
| Retrofit + OkHttp | 网络请求 |
| Coil | 图片加载 |
| DataStore | 本地数据持久化 (Token) |
| Navigation Compose | 页面导航 |
| MVVM | 架构模式 |

## 功能模块

### 📸 照片管理
- 照片网格浏览（3列瀑布流）
- 标签筛选、关键词搜索
- 照片详情查看（原图、EXIF 信息、城市、拍摄时间）
- 照片上传（支持多选 + 标签 + 城市）
- 照片评论/留言
- 照片删除（移入回收站）
- 照片点击追踪（推荐算法）

### 🎬 视频管理
- 视频网格浏览
- 视频上传
- 视频播放入口

### 🤖 AI 对话
- 多轮对话
- 创建/删除对话会话
- 消息发送与接收
- AI 思考状态指示

### 📁 文件存储
- 任意文件上传
- 文件列表浏览
- 文件删除

### 📚 图书管理
- 电子书上传（EPUB/PDF）
- 图书列表浏览
- 图书删除

### 👨‍👩‍👧‍👦 家庭共享
- 创建家庭
- 邀请码加入家庭
- 家庭成员管理
- 共享照片浏览

### 👥 好友管理
- 搜索用户
- 发送/接受/拒绝好友请求
- 好友列表管理

### 🧑 人脸识别
- 人脸聚类列表
- 聚类照片浏览
- 重命名/删除聚类

### 🏷️ 标签管理
- 标签 CRUD
- 颜色标识

### 🗑️ 回收站
- 已删除照片浏览
- 恢复照片
- 永久删除
- 清空回收站

### ⚙️ 设置 & 个人
- 个人资料查看
- 服务器地址配置
- 主题跟随系统（深色/浅色）
- 退出登录

## 项目结构

```
smart-nas-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/smartnas/app/
│       │   ├── SmartNASApp.kt          # Application 入口
│       │   ├── MainActivity.kt          # 主 Activity
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── SmartNASApi.kt   # Retrofit API 接口定义
│       │   │   │   └── RetrofitHolder.kt # 动态 URL Retrofit 管理
│       │   │   ├── model/
│       │   │   │   └── Models.kt        # 数据模型
│       │   │   └── repository/
│       │   │       └── AuthRepository.kt # 认证仓库
│       │   ├── di/
│       │   │   └── AppModule.kt         # Hilt 依赖注入模块
│       │   ├── ui/
│       │   │   ├── components/
│       │   │   │   └── CommonUI.kt      # 通用 UI 组件
│       │   │   ├── navigation/
│       │   │   │   └── NavGraph.kt      # 导航路由
│       │   │   ├── screens/
│       │   │   │   ├── login/           # 登录
│       │   │   │   ├── home/            # 首页仪表盘
│       │   │   │   ├── photo/           # 照片管理
│       │   │   │   ├── video/           # 视频管理
│       │   │   │   ├── aichat/          # AI 对话
│       │   │   │   ├── file/            # 文件存储
│       │   │   │   ├── book/            # 图书管理
│       │   │   │   ├── family/          # 家庭共享
│       │   │   │   ├── friend/          # 好友管理
│       │   │   │   ├── face/            # 人脸识别
│       │   │   │   ├── tags/            # 标签管理
│       │   │   │   ├── recycle/         # 回收站
│       │   │   │   ├── settings/        # 设置
│       │   │   │   └── profile/         # 个人资料
│       │   │   └── theme/
│       │   │       └── Theme.kt         # Material 3 主题
│       │   └── util/
│       │       ├── TokenManager.kt      # JWT Token 管理
│       │       ├── Resource.kt          # 通用资源密封类
│       │       └── FormatUtil.kt        # 格式化工具
│       └── res/                         # 资源文件
├── build.gradle.kts                     # 根构建脚本
├── settings.gradle.kts                  # 项目设置
├── gradle.properties                    # Gradle 属性
└── README.md
```

## 快速开始

### 环境要求

- Android Studio Ladybug (2024.2) 或更高版本
- JDK 17+
- Android SDK 35

### 运行步骤

1. **用 Android Studio 打开项目**
   ```
   File → Open → 选择 smart-nas-android 目录
   ```

2. **配置服务器地址**
   - 启动 App 后，在登录页点击「服务器设置」
   - 输入你的 Smart NAS 后端地址，如 `http://192.168.1.100:8080`
   - 默认地址为 `http://10.0.2.2:8080`（Android 模拟器访问宿主机）

3. **登录**
   
   - 默认账号：`admin / admin`

### 构建 APK

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需要签名配置）
./gradlew assembleRelease
```

APK 输出路径：`app/build/outputs/apk/`

## 网络配置

### 开发环境

- Android 模拟器使用 `10.0.2.2` 访问宿主机 `localhost`
- 真机需要使用电脑的局域网 IP

### 生产环境

- 在登录页「服务器设置」中修改服务器地址
- 支持 HTTP 和 HTTPS
- 如使用自签名证书，需配置网络安全策略

## API 对接

所有 API 接口与 Web 端完全一致，详见 `SmartNASApi.kt`。主要模块：

| 模块 | API 前缀 | 说明 |
|---|---|---|
| 认证 | `/api/auth/` | 登录/登出/用户信息 |
| 照片 | `/api/photo/` | 上传/列表/详情/删除/搜索 |
| 标签 | `/api/tag/` | 标签 CRUD |
| AI 对话 | `/api/ai-chat/` | 会话/消息 |
| 文件 | `/api/file/` | 文件上传/下载/删除 |
| 图书 | `/api/book/` | 图书上传/管理 |
| 家庭 | `/api/family/` | 家庭/成员/共享 |
| 好友 | `/api/friend/` | 搜索/请求/列表 |
| 人脸 | `/api/face/` | 聚类/照片 |
| 回收站 | `/api/recycle/` | 恢复/永久删除 |
| 城市 | `/api/city/` | 城市列表/统计 |
| AI 模型 | `/api/ai-model/` | 模型配置 |

## License

与 Smart NAS 主项目一致。
