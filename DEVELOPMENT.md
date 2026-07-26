# Smart-NAS 开发文档

本文档详细说明如何从零开始为 Smart-NAS 添加一个新页面或新功能。

---

## 目录

1. [项目架构概览](#1-项目架构概览)
2. [添加新页面（前端）](#2-添加新页面前端)
3. [添加新 API 接口（后端）](#3-添加新-api-接口后端)
4. [添加数据库表](#4-添加数据库表)
5. [添加 i18n 国际化](#5-添加-i18n-国际化)
6. [添加路由和菜单](#6-添加路由和菜单)
7. [完整示例：添加"任务管理"页面](#7-完整示例添加任务管理页面)
8. [常见问题](#8-常见问题)

---

## 1. 项目架构概览

```
smart-nas/
├── gateway/                    # Spring Cloud Gateway (端口 8080)
│   └── src/main/java/yzh/stock/gateway/
│       ├── GatewayApplication.java
│       └── filter/AuthGlobalFilter.java
│
├── service-business/           # 核心业务服务 (端口 8081)
│   └── src/main/java/yzh/stock/business/
│       ├── controller/         # 19 个 REST 控制器
│       ├── service/            # 18 个业务服务
│       ├── entity/             # 20+ 个实体类
│       ├── mapper/             # MyBatis Mapper 接口
│       ├── utils/              # 工具类（PasswordUtil 等）
│       └── config/             # 配置类（WebDAV、数据初始化等）
│
├── service-ai-analyze/         # Python AI 服务 (端口 8000)
│   ├── main.py
│   └── app/
│       ├── routers/            # API 路由
│       └── services/           # 业务逻辑
│
├── web-vue/                    # Vue 3 前端 (端口 5173)
│   └── src/
│       ├── api/index.js        # 所有 API 接口定义
│       ├── assets/i18n/        # 国际化文件 (zh.json, en.json)
│       ├── components/         # 公共组件
│       ├── router/             # 路由配置
│       ├── utils/              # 工具函数
│       └── views/              # 20+ 个页面组件
│
└── middleware-compose/         # 中间件 Docker Compose
    ├── docker-compose.yml
    └── init-sql/
        ├── init.sql            # 初始化 SQL
        └── migration-*.sql     # 数据库迁移脚本
```

**请求流转：**
```
前端 (5173) → Gateway (8080) → Business Service (8081) → 数据库
                                    ↓
                              AI Service (8000) → Ollama (11434)
```

**密码安全：**
- 系统使用 BCrypt 单向哈希存储密码
- 首次启动时自动迁移旧密码（AES/明文 → BCrypt）
- 工具类：`utils/PasswordUtil.java`

---

## 2. 添加新页面（前端）

### 2.1 创建页面组件

在 `web-vue/src/views/` 下创建新目录和 `index.vue`：

```
web-vue/src/views/
└── your-module/
    └── index.vue
```

**模板示例：**

```vue
<template>
  <div class="your-page" :key="lang">
    <el-card>
      <template #header>{{ t('yourModule.title') }}</template>
      <!-- 页面内容 -->
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from '../../utils/i18n'

const { t, lang } = useI18n()

// 数据
const dataList = ref([])

// 加载数据
onMounted(async () => {
  await loadData()
})

async function loadData() {
  // 调用 API 获取数据
}
</script>

<style scoped>
.your-page { max-width: 700px; margin: 0 auto; padding: 0 12px; }
</style>
```

### 2.2 添加 API 接口

在 `web-vue/src/api/index.js` 中添加接口定义：

```javascript
// Your Module
export const getYourList = (params) => request.get('/api/your-module/list', { params })
export const createYourItem = (data) => request.post('/api/your-module', data)
export const updateYourItem = (id, data) => request.put(`/api/your-module/${id}`, data)
export const deleteYourItem = (id) => request.delete(`/api/your-module/${id}`)
```

### 2.3 在页面中使用 API

```vue
<script setup>
import { getYourList, createYourItem } from '../../api'

async function loadData() {
  const res = await getYourList()
  if (res.code === 200) dataList.value = res.data
}

async function handleCreate(data) {
  const res = await createYourItem(data)
  if (res.code === 200) {
    ElMessage.success(t('common.success'))
    loadData()
  }
}
</script>
```

---

## 3. 添加新 API 接口（后端）

### 3.1 创建实体类

在 `service-business/src/main/java/yzh/stock/business/entity/` 下创建：

```java
package yzh.stock.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("your_table")
public class YourEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

### 3.2 创建 Mapper

在 `service-business/src/main/java/yzh/stock/business/mapper/` 下创建：

```java
package yzh.stock.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yzh.stock.business.entity.YourEntity;

@Mapper
public interface YourEntityMapper extends BaseMapper<YourEntity> {
}
```

### 3.3 创建 Service

在 `service-business/src/main/java/yzh/stock/business/service/` 下创建：

```java
package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.YourEntity;
import yzh.stock.business.mapper.YourEntityMapper;

import java.util.List;

@Service
public class YourEntityService {

    private final YourEntityMapper mapper;

    public YourEntityService(YourEntityMapper mapper) {
        this.mapper = mapper;
    }

    public List<YourEntity> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<YourEntity>().orderByDesc(YourEntity::getCreateTime));
    }

    public YourEntity getById(Long id) {
        return mapper.selectById(id);
    }

    public void create(YourEntity entity) {
        mapper.insert(entity);
    }

    public void update(YourEntity entity) {
        mapper.updateById(entity);
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
```

### 3.4 创建 Controller

在 `service-business/src/main/java/yzh/stock/business/controller/` 下创建：

```java
package yzh.stock.business.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.stock.business.entity.YourEntity;
import yzh.stock.business.service.YourEntityService;

import java.util.Map;

@RestController
@RequestMapping("/api/your-module")
public class YourEntityController {

    private final YourEntityService service;

    public YourEntityController(YourEntityService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(Map.of("code", 200, "data", service.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", service.getById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody YourEntity entity) {
        service.create(entity);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody YourEntity entity) {
        entity.setId(id);
        service.update(entity);
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
```

### 3.5 配置 Gateway 路由

在 `gateway/src/main/resources/application.yml` 中添加路由：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: your-module
          uri: lb://service-business
          predicates:
            - Path=/api/your-module/**
```

---

## 4. 添加数据库表

### 4.1 编写 SQL

在 `middleware-compose/init-sql/init.sql` 中添加：

```sql
CREATE TABLE IF NOT EXISTS `your_table` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '名称',
  `description` TEXT COMMENT '描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='你的表';
```

**现有数据库表（完整列表）：**

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

### 4.2 执行 SQL

```bash
# 进入 MySQL 容器
docker exec -it smart-nas-mysql mysql -u root -proot

# 执行 SQL
source /docker-entrypoint-initdb.d/init.sql
```

或直接在 Navicat 等工具中执行。

---

## 5. 添加 i18n 国际化

### 5.1 中文翻译

在 `web-vue/src/assets/i18n/zh.json` 中添加：

```json
{
  "yourModule": {
    "title": "你的模块",
    "name": "名称",
    "description": "描述",
    "actions": "操作",
    "create": "新建",
    "confirmDelete": "确定删除？"
  }
}
```

### 5.2 英文翻译

在 `web-vue/src/assets/i18n/en.json` 中添加：

```json
{
  "yourModule": {
    "title": "Your Module",
    "name": "Name",
    "description": "Description",
    "actions": "Actions",
    "create": "Create",
    "confirmDelete": "Delete this item?"
  }
}
```

### 5.3 在页面中使用

```vue
<template>
  <div>{{ t('yourModule.title') }}</div>
</template>

<script setup>
const { t } = useI18n()
</script>
```

---

## 6. 添加路由和菜单

### 6.1 添加路由

在 `web-vue/src/router/index.js` 中添加：

```javascript
{
  path: '/your-module',
  name: 'YourModule',
  component: () => import('../views/your-module/index.vue'),
  meta: { title: 'yourModule.title', icon: 'Setting', requiresAuth: true }
}
```

### 6.2 添加菜单

在 `web-vue/src/components/Layout/index.vue` 的菜单配置中添加：

```javascript
{
  path: '/your-module',
  title: 'yourModule.title',
  icon: 'Setting'
}
```

或在数据库 `sys_permission` 表中添加菜单权限：

```sql
INSERT INTO sys_permission (perm_name, perm_key, type, route_path, icon, sort_order)
VALUES ('你的模块', 'your-module', 'menu', '/your-module', 'Setting', 100);
```

---

## 7. 完整示例：添加"任务管理"页面

以下是一个完整的端到端示例，展示如何添加一个简单的任务管理功能。

### 步骤 1：创建数据库表

```sql
CREATE TABLE IF NOT EXISTS `task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
  `description` TEXT COMMENT '任务描述',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待办 1-进行中 2-已完成',
  `priority` TINYINT DEFAULT 0 COMMENT '优先级: 0-低 1-中 2-高',
  `due_date` DATETIME COMMENT '截止时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';
```

### 步骤 2：后端代码

**实体类 `Task.java`：**

```java
package yzh.stock.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private Integer status;
    private Integer priority;
    private LocalDateTime dueDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**Mapper `TaskMapper.java`：**

```java
package yzh.stock.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yzh.stock.business.entity.Task;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
```

**Service `TaskService.java`：**

```java
package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.Task;
import yzh.stock.business.mapper.TaskMapper;

import java.util.List;

@Service
public class TaskService {
    private final TaskMapper mapper;

    public TaskService(TaskMapper mapper) {
        this.mapper = mapper;
    }

    public List<Task> listByUser(Long userId) {
        return mapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .orderByDesc(Task::getCreateTime)
        );
    }

    public void create(Task task) {
        mapper.insert(task);
    }

    public void update(Task task) {
        mapper.updateById(task);
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
```

**Controller `TaskController.java`：**

```java
package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.stock.business.entity.Task;
import yzh.stock.business.service.TaskService;

import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", service.listByUser(userId)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Task task, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        task.setUserId(userId);
        service.create(task);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        service.update(task);
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
```

### 步骤 3：前端代码

**API 接口 `api/index.js`：**

```javascript
// Task
export const getTaskList = () => request.get('/api/task/list')
export const createTask = (data) => request.post('/api/task', data)
export const updateTask = (id, data) => request.put(`/api/task/${id}`, data)
export const deleteTask = (id) => request.delete(`/api/task/${id}`)
```

**页面 `views/task/index.vue`：**

```vue
<template>
  <div class="task-page" :key="lang">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ t('task.title') }}</span>
          <el-button type="primary" size="small" @click="showCreateDialog">{{ t('task.create') }}</el-button>
        </div>
      </template>
      <el-table :data="taskList" stripe>
        <el-table-column :label="task.title" prop="title" />
        <el-table-column :label="task.status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'warning' : 'info'">
              {{ row.status === 2 ? '已完成' : row.status === 1 ? '进行中' : '待办' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="task.actions" width="150">
          <template #default="{ row }">
            <el-button size="small" link @click="editTask(row)">{{ t('common.edit') }}</el-button>
            <el-popconfirm :title="t('task.confirmDelete')" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingTask ? t('common.edit') : t('task.create')" width="500px">
      <el-form label-width="80px">
        <el-form-item :label="task.title">
          <el-input v-model="taskForm.title" />
        </el-form-item>
        <el-form-item :label="task.description">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="task.priority">
          <el-select v-model="taskForm.priority">
            <el-option label="低" :value="0" />
            <el-option label="中" :value="1" />
            <el-option label="高" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from '../../utils/i18n'
import { getTaskList, createTask, updateTask, deleteTask } from '../../api'
import { ElMessage } from 'element-plus'

const { t, lang } = useI18n()
const taskList = ref([])
const dialogVisible = ref(false)
const editingTask = ref(null)
const taskForm = ref({ title: '', description: '', priority: 0 })

onMounted(() => loadData())

async function loadData() {
  const res = await getTaskList()
  if (res.code === 200) taskList.value = res.data
}

function showCreateDialog() {
  editingTask.value = null
  taskForm.value = { title: '', description: '', priority: 0 }
  dialogVisible.value = true
}

function editTask(task) {
  editingTask.value = task
  taskForm.value = { title: task.title, description: task.description, priority: task.priority }
  dialogVisible.value = true
}

async function handleSave() {
  if (editingTask.value) {
    await updateTask(editingTask.value.id, taskForm.value)
  } else {
    await createTask(taskForm.value)
  }
  ElMessage.success(t('common.success'))
  dialogVisible.value = false
  loadData()
}

async function handleDelete(id) {
  await deleteTask(id)
  ElMessage.success(t('common.success'))
  loadData()
}
</script>
```

**i18n 翻译：**

`zh.json`：
```json
{
  "task": {
    "title": "任务管理",
    "create": "新建任务",
    "name": "任务名称",
    "description": "描述",
    "status": "状态",
    "priority": "优先级",
    "actions": "操作",
    "confirmDelete": "确定删除此任务？"
  }
}
```

`en.json`：
```json
{
  "task": {
    "title": "Task Management",
    "create": "New Task",
    "name": "Task Name",
    "description": "Description",
    "status": "Status",
    "priority": "Priority",
    "actions": "Actions",
    "confirmDelete": "Delete this task?"
  }
}
```

**路由配置 `router/index.js`：**

```javascript
{
  path: '/task',
  name: 'Task',
  component: () => import('../views/task/index.vue'),
  meta: { title: 'task.title', icon: 'List', requiresAuth: true }
}
```

---

## 8. 常见问题

### Q: 页面空白，没有数据显示？

检查：
1. API 接口路径是否正确
2. Gateway 路由是否配置
3. 浏览器控制台是否有报错
4. 后端服务是否启动

### Q: 401 未授权？

检查：
1. 是否在 Gateway 白名单中添加了新接口（如不需要登录）
2. 请求头中是否携带了 `Authorization: Bearer <token>`
3. JWT 是否过期

### Q: 跨域错误？

Gateway 已配置全局跨域，如仍有问题，检查 `AuthGlobalFilter.java` 中的 CORS 配置。

### Q: 数据库表创建失败？

检查：
1. MySQL 容器是否运行：`docker ps | grep mysql`
2. SQL 语法是否正确
3. 字符集是否为 `utf8mb4`

### Q: 前端路由 404？

检查：
1. 路由路径是否以 `/` 开头
2. 组件路径是否正确
3. 是否在 `router/index.js` 中添加了路由

### Q: i18n 文本没有翻译？

检查：
1. `zh.json` 和 `en.json` 中是否都添加了对应的 key
2. key 是否拼写正确
3. 是否使用了 `t('key')` 函数

---

## 开发规范

### 代码风格

- **Java**：遵循 Google Java Style Guide
- **JavaScript/Vue**：遵循 ESLint 默认规则
- **命名**：
  - Java：驼峰命名（`YourEntity`）
  - 数据库：蛇形命名（`your_table`）
  - Vue 组件：PascalCase（`YourComponent.vue`）

### 提交规范

```
feat: 新增功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
test: 测试相关
chore: 构建/工具相关
```

### 分支管理

- `main`：生产环境
- `develop`：开发环境
- `feature/*`：功能分支
- `fix/*`：修复分支
