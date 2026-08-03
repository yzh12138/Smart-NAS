# Smart NAS iOS 客户端

基于 Smart NAS 后端 API 的 iOS 原生客户端，使用 Swift + SwiftUI + async/await 构建。

---

## 一、环境要求

| 组件 | 最低版本 | 说明 |
|---|---|---|
| Xcode | 15.4+ | 推荐 Xcode 16+ |
| iOS 部署目标 | 17.0 | 最低支持版本 |
| Swift | 5.9+ | Xcode 自带 |
| macOS | 14.0+ | Sonoma 或更新 |

---

## 二、获取项目

```bash
# 克隆项目
git clone https://gitee.com/yin-zhenhua/smart-nas.git

# iOS 客户端在 smart-nas-ios/ 目录
cd smart-nas/smart-nas-ios
```

---

## 三、打开项目

### 方式 A：Xcode（推荐）

```bash
# 直接用 Xcode 打开
open SmartNAS/SmartNAS.xcodeproj
```

或在 Xcode 中：`File → Open → 选择 SmartNAS/SmartNAS.xcodeproj`

### 方式 B：命令行构建

```bash
cd SmartNAS

# 构建（模拟器）
xcodebuild -scheme SmartNAS -destination 'platform=iOS Simulator,name=iPhone 16' build

# 构建（真机）
xcodebuild -scheme SmartNAS -destination 'generic/platform=iOS' build
```

---

## 四、配置服务器地址

### 4.1 默认配置

默认服务器地址为 `http://localhost:8080`。

### 4.2 修改默认地址

编辑 `SmartNAS/App/SmartNASApp.swift`：

```swift
@Published var serverURL: String = UserDefaults.standard.string(forKey: "server_url") ?? "http://192.168.1.100:8080"
```

### 4.3 运行时修改

启动 App 后，在登录页展开「服务器设置」，输入后端地址。

### 4.4 网络环境说明

| 场景 | 服务器地址 |
|---|---|
| iOS 模拟器访问宿主机 | `http://localhost:8080` |
| 真机访问同局域网电脑 | `http://192.168.x.x:8080` |
| 远程服务器 | `http://your-server.com:8080` |

> ⚠️ iOS 17+ 默认阻止 HTTP 明文请求。`Info.plist` 已配置 `NSAllowsArbitraryLoads = true` 允许 HTTP。
> 生产环境建议使用 HTTPS。

---

## 五、运行

### 5.1 模拟器运行

1. Xcode 顶部选择模拟器（如 `iPhone 16`）
2. 点击 ▶ Run 按钮（⌘R）
3. 等待编译完成（首次约 1-2 分钟）

### 5.2 真机运行

1. iPhone 用 USB 连接 Mac
2. iPhone 上信任此电脑（`设置 → 通用 → VPN与设备管理 → 信任`）
3. Xcode 顶部选择你的 iPhone
4. 点击 ▶ Run（⌘R）
5. 如提示签名错误：
   - `Signing & Capabilities → Team` 选择你的 Apple ID
   - 或创建免费开发者账号

### 5.3 命令行运行

```bash
# 列出可用模拟器
xcrun simctl list devices available

# 启动模拟器
xcrun simctl boot "iPhone 16"

# 构建并运行
xcodebuild -scheme SmartNAS -destination 'platform=iOS Simulator,name=iPhone 16' build
```

---

## 六、打包发布

### 6.1 Archive 打包

1. Xcode → `Product → Archive`
2. 完成后在 `Window → Organizer` 中看到 archive
3. 点击 `Distribute App`：
   - **Development**：开发测试用
   - **Ad Hoc**：指定设备安装
   - **App Store**：上架 App Store
   - **Enterprise**：企业分发

### 6.2 导出 IPA

```bash
# Archive
xcodebuild -scheme SmartNAS -archivePath build/SmartNAS.xcarchive archive

# 导出 IPA（需要 ExportOptions.plist）
xcodebuild -exportArchive \
  -archivePath build/SmartNAS.xcarchive \
  -exportPath build/output \
  -exportOptionsPlist ExportOptions.plist
```

### 6.3 TestFlight 测试

1. Archive 后选择 `Distribute App → App Store Connect`
2. 上传到 App Connect
3. 在 App Store Connect 中添加测试员
4. 测试员在 TestFlight App 中安装

---

## 七、功能模块

| 模块 | 功能 |
|---|---|
| 🔐 登录 | 服务器地址配置、JWT 登录、Keychain Token 存储 |
| 🏠 首页 | 统计卡片、快捷操作、城市分布、最近照片 |
| 📸 照片 | 网格浏览、标签筛选、搜索、详情、评论、上传、删除 |
| 🎬 视频 | 视频网格、详情查看 |
| 🤖 AI 对话 | 多轮对话、会话管理、消息发送 |
| 📁 文件 | 文件上传、列表、删除 |
| 📚 图书 | 图书列表、删除 |
| 👨‍👩‍👧‍👦 家庭 | 创建/加入家庭、成员管理、共享照片 |
| 👥 好友 | 搜索用户、请求管理、好友列表 |
| 🧑 人脸 | 聚类浏览、重命名、删除 |
| 🏷️ 标签 | 标签 CRUD、颜色标识 |
| 🗑️ 回收站 | 恢复、永久删除、清空 |
| ⚙️ 设置 | 功能入口、退出登录 |
| 👤 个人 | 资料查看 |

---

## 八、项目结构

```
smart-nas-ios/
└── SmartNAS/
    ├── SmartNAS.xcodeproj/           # Xcode 项目文件
    │   └── project.pbxproj
    ├── App/
    │   └── SmartNASApp.swift         # App 入口 + AppState + RootView
    ├── Models/
    │   └── Models.swift              # 所有数据模型（Codable）
    ├── Services/
    │   ├── APIService.swift          # 网络请求层（async/await）
    │   └── KeychainHelper.swift      # Keychain 工具
    ├── Components/
    │   └── CommonViews.swift         # 通用 UI 组件
    ├── Theme/
    │   └── Theme.swift               # 颜色、样式、图片 URL
    ├── Views/
    │   ├── Login/LoginView.swift
    │   ├── Home/HomeView.swift
    │   ├── Photo/
    │   │   ├── PhotoGalleryView.swift
    │   │   ├── PhotoDetailView.swift
    │   │   └── PhotoUploadView.swift
    │   ├── Video/VideoView.swift
    │   ├── AIChat/AIChatView.swift
    │   ├── File/FileView.swift
    │   ├── Book/BookView.swift
    │   ├── Family/FamilyView.swift
    │   ├── Friend/FriendView.swift
    │   ├── Face/FaceView.swift
    │   ├── Tags/TagsView.swift
    │   ├── Recycle/RecycleView.swift
    │   ├── Settings/SettingsView.swift
    │   └── Profile/ProfileView.swift
    ├── Info.plist                     # 应用配置
    └── Resources/                     # 资源文件
```

---

## 九、常见问题

### Q: 编译报 "No such module"？
- `File → Packages → Reset Package Contents`
- 或 `Product → Clean Build Folder` (⇧⌘K) 后重新编译

### Q: 真机运行报签名错误？
- `Signing & Capabilities → Team` 选择你的 Apple ID
- 免费账号每 7 天需要重新签名
- 付费开发者账号（$99/年）无此限制

### Q: 网络请求失败？
- 检查 `Info.plist` 中 `NSAllowsArbitraryLoads` 是否为 `true`
- 检查后端服务是否已启动
- 真机需要使用局域网 IP

### Q: 照片上传失败？
- 检查 `Info.plist` 中 `NSPhotoLibraryUsageDescription` 是否存在
- 首次使用需要授权照片库访问权限

### Q: 模拟器中键盘无法输入？
- `I/O → Keyboard → Connect Hardware Keyboard` 取消勾选

---

## 十、技术栈详情

| 技术 | 版本 | 用途 |
|---|---|---|
| Swift | 5.9+ | 开发语言 |
| SwiftUI | iOS 17+ | 声明式 UI |
| async/await | Swift 5.5+ | 异步网络请求 |
| URLSession | 系统自带 | HTTP 客户端 |
| PhotosPicker | iOS 16+ | 系统照片选择器 |
| Keychain | Security 框架 | Token 安全存储 |
| NavigationStack | iOS 16+ | 页面导航 |

---

## 十一、与 Android 版的差异

| 功能 | Android | iOS |
|---|---|---|
| 照片上传 | 多选 + 标签 + 城市 | PhotosPicker + 标签 + 城市 |
| 文件上传 | 系统文�