# MIMO Smart-NAS — AI 服务详细文档

本文档详细记录 Python AI 服务 (`service-ai-analyze/`) 的架构、Prompt 设计、用户记忆机制、模型配置和扩展方式。

---

## 目录

1. [服务架构总览](#1-服务架构总览)
2. [目录结构](#2-目录结构)
3. [Prompt 一览](#3-prompt-一览)
4. [用户记忆机制](#4-用户记忆机制)
5. [模型配置与切换](#5-模型配置与切换)
6. [API 接口详解](#6-api-接口详解)
7. [环境变量与配置](#7-环境变量与配置)
8. [常见问题与调试](#8-常见问题与调试)

---

## 1. 服务架构总览

```
┌─────────────┐     HTTP/1.1      ┌──────────────────┐     HTTP       ┌──────────────┐
│   前端 Vue  │ ────────────────→ │  Java 后端       │ ────────────→ │  Python AI   │
│  (端口 5173) │                   │  (端口 8081)     │               │  (端口 8000) │
└─────────────┘                   └──────────────────┘               └──────┬───────┘
                                                                          │
                                                                          │ HTTP
                                                                          ▼
                                                                   ┌──────────────┐
                                                                   │   Ollama     │
                                                                   │  (端口 11434)│
                                                                   │  Qwen2.5     │
                                                                   │  Qwen2.5vl   │
                                                                   └──────────────┘
```

**调用链路：**
- 前端 → Gateway (8080) → Java Business Service (8081) → Python AI Service (8000) → Ollama (11434)
- AI 图片分析：Java 端通过 `PhotoService` 调用 Python 的 `/api/ai/analyze-image`
- AI 对话：Java 端通过 `AiChatService` 调用 Python 的 `/api/ai/chat`

**注意事项：**
- Java `HttpClient` 必须使用 HTTP/1.1（`HttpClient.Version.HTTP_1_1`），不能用默认的 HTTP/2 h2c，否则 uvicorn 会收到空请求体
- Python 服务直连 Ollama，不经过 Gateway

---

## 2. 目录结构

```
service-ai-analyze/
├── main.py                          # FastAPI 入口，注册路由，配置日志
├── requirements.txt                 # Python 依赖
├── ai_service.log                   # 运行时日志文件
│
├── app/
│   ├── routers/                     # API 路由层
│   │   ├── chat.py                  #   POST /api/ai/chat       — AI 对话
│   │   │                            #   POST /api/ai/analyze-image — AI 图片分析
│   │   ├── thumbnail.py             #   POST /api/ai/thumbnail  — 缩略图生成
│   │   ├── exif_parser.py           #   POST /api/ai/parse-exif — EXIF 解析
│   │   └── ai_analyzer.py           #   （已废弃，功能合并到 chat.py）
│   │
│   └── services/                    # 业务逻辑层
│       ├── ai_client.py             #   通用 AI 调用客户端（chat_completion + analyze_image）
│       └── ollama_client.py         #   Ollama 直连客户端（早期版本，已被 ai_client 替代）
```

---

## 3. Prompt 一览

### 3.1 AI 图片分析 Prompt

**位置：** `app/services/ai_client.py` → `analyze_image()` 函数（第 55-65 行）

```python
default_prompt = """请分析这张照片，返回 JSON 格式的结果：
{
  "tags": ["标签1", "标签2", ...],
  "city": "城市名（如果能识别出拍摄地点）",
  "province": "省份名",
  "description": "一句话描述这张照片"
}
注意：
- tags 是照片相关标签，如风景、建筑、人物、美食等，3-5个
- 如果无法识别地点，city 和 province 设为 null
- 只返回 JSON，不要其他文字"""
```

**使用模型：** `qwen2.5vl:7b`（视觉语言模型）
**调用方式：** Ollama `/api/generate` 接口（非 `/api/chat`），传入 Base64 编码的图片
**参数：** `temperature=0.3`, `num_predict=512`

**后处理逻辑：**
1. 去除 markdown 代码块标记（```json ... ```）
2. 尝试 JSON 解析
3. 解析失败时返回容错结果 `{"tags": ["AI识别"], "city": null, ...}`

### 3.2 AI 对话 System Prompt（5 层优先级链）

**位置：** `service-business/.../AiChatService.java` → `sendMessage()` 方法

系统采用多层提示词优先级链，确保灵活的提示词管理：

**优先级链（从高到低）：**

```
1. 模型 promptTemplate（前缀）
   ↓ 如果为空则跳过
2. 对话级记忆提示词（ai_conversation.system_prompt）
   ↓ 如果为空则跳过
3. 用户级自定义提示词（sys_user.ai_prompt）
   ↓ 如果为空则跳过
4. 全局提示词（ai_prompt.json 文件）
   ↓ 如果文件不存在则使用
5. 默认硬编码提示词（"You are a helpful assistant..."）
```

**实现逻辑：**
```java
// 构建 system prompt（优先级：对话记忆 > 用户提示词 > 全局提示词）
String systemPrompt;
if (conversationPrompt != null && !conversationPrompt.isEmpty()) {
    systemPrompt = conversationPrompt;  // 对话级记忆优先
} else if (userPrompt != null && !userPrompt.isEmpty()) {
    systemPrompt = userPrompt;  // 用户级提示词次之
} else {
    systemPrompt = globalPrompt;  // 全局提示词兜底
}

// 如果模型有 promptTemplate，附加在 system prompt 之前
if (model.getPromptTemplate() != null && !model.getPromptTemplate().isEmpty()) {
    systemPrompt = model.getPromptTemplate() + "\n\n" + systemPrompt;
}
```

**说明：**
- 每个对话会话创建时，从用户配置或全局配置读取 system prompt
- 存储在数据库 `ai_conversation.system_prompt` 字段
- 每次发送消息时，Java 端将 system prompt 作为第一条消息（`role: "system"`）发送给 Python
- 用户可通过 `/api/ai-prompt` 接口管理自定义提示词
- 管理员可通过 `/api/ai-model/global-prompt` 接口修改全局提示词

### 3.3 修改 Prompt 的方式

| 场景 | 修改位置 | 说明 |
|---|---|---|
| 修改图片分析 prompt | `service-ai-analyze/app/services/ai_client.py` 第 101 行 | 修改 `default_prompt` 变量 |
| 修改全局 system prompt | `ai_prompt.json` 文件（`ai.config-dir` 目录下） | 管理员通过 API 或直接编辑 |
| 修改用户级提示词 | `sys_user.ai_prompt` 字段 | 用户通过个人资料页面编辑 |
| 修改对话级提示词 | `ai_conversation.system_prompt` 字段 | 用户通过 AI 对话页面编辑 |
| 使用模型自带的 prompt 模板 | 数据库 `ai_model_config.prompt_template` 字段 | 模型级前缀，附加在最前面 |

---

## 4. 用户记忆机制

### 4.1 当前实现

Smart-NAS 的 AI 对话**没有独立的持久化记忆系统**。每个用户的"记忆"通过以下方式实现：

```
用户发送消息
    ↓
Java 后端将消息存入 MySQL（ai_message 表）
    ↓
下次发送时，加载该对话的所有历史消息
    ↓
将完整历史（system prompt + 所有 user/assistant 消息）发送给 Python
    ↓
Python 转发给 Ollama，Ollama 基于完整上下文生成回复
```

**关键点：**
- **会话级记忆**：每个对话（`ai_conversation`）独立维护消息历史
- **用户级隔离**：通过 `ai_conversation.user_id` 字段隔离不同用户
- **无跨会话记忆**：切换对话后，之前的上下文不保留
- **无向量数据库**：没有 RAG、向量检索等长期记忆机制

### 4.2 数据库表结构

**ai_conversation（对话会话表）：**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `user_id` | BIGINT | 所属用户 ID |
| `title` | VARCHAR | 对话标题（首条消息自动截取前 50 字） |
| `model_config_id` | BIGINT | 关联的 AI 模型配置 ID |
| `system_prompt` | TEXT | 系统提示词 |
| `workspace_path` | VARCHAR | 工作空间路径（预留，暂未使用） |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |

**ai_message（对话消息表）：**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `conversation_id` | BIGINT | 所属对话 ID |
| `role` | VARCHAR | 角色：`user` / `assistant` / `system` |
| `content` | TEXT | 消息内容 |
| `tokens` | INT | Token 数量（预留，暂未填充） |
| `create_time` | DATETIME | 创建时间 |

### 4.3 消息发送流程

```
前端 POST /api/ai-chat/conversation/{id}/send
  body: { "content": "用户输入" }
    ↓
Java AiChatService.sendMessage()
  1. 保存 user 消息到 ai_message 表
  2. 从数据库加载该对话的所有历史消息
  3. 构建 messages 数组：
     [
       { "role": "system",    "content": "You are a helpful assistant..." },
       { "role": "user",      "content": "消息1" },
       { "role": "assistant", "content": "回复1" },
       { "role": "user",      "content": "消息2" },
       ...
     ]
  4. 调用 Python POST /api/ai/chat
  5. Python 转发给 Ollama
  6. 保存 assistant 回复到 ai_message 表
  7. 返回给前端
```

### 4.4 后续扩展方向

如需更智能的记忆系统，可考虑：

1. **自动标题生成**：用 AI 为每个对话生成标题（目前用首条消息截取）
2. **对话摘要**：历史消息过多时，用 AI 生成摘要替代完整历史
3. **向量记忆（RAG）**：引入 ChromaDB / FAISS，存储用户历史对话的向量嵌入，实现跨会话记忆
4. **用户画像**：基于历史对话提取用户偏好，注入 system prompt
5. **workspace 扩展**：利用已预留的 `workspace_path` 字段，为每个对话创建独立文件存储上下文

---

## 5. 模型配置与切换

### 5.1 数据库配置

模型配置存储在 `ai_model_config` 表中：

| 字段 | 说明 | 示例 |
|---|---|---|
| `model_name` | 显示名称 | Qwen2.5vl |
| `model_type` | 类型：`ollama` 或 `openai` | ollama |
| `model_id` | 模型标识 | qwen2.5vl:7b |
| `api_url` | API 地址 | http://localhost:11434 |
| `api_key` | API 密钥（OpenAI 兼容时使用） | sk-xxx |
| `prompt_template` | 提示词模板（预留） | - |
| `is_default` | 是否默认模型（0/1） | 1 |
| `status` | 状态（0/1） | 1 |

### 5.2 支持的模型类型

**Ollama 类型（`model_type: "ollama"`）：**
- 聊天：`POST {api_url}/api/chat`
- 图片分析：`POST {api_url}/api/generate`（使用 `qwen2.5vl:7b`）

**OpenAI 兼容类型（`model_type: "openai"`）：**
- 聊天：`POST {api_url}/v1/chat/completions`
- 图片分析：暂不支持（硬编码使用 Ollama）

**支持的模型类型（13+ 种）：**

| 类型 | 模型 ID | 说明 |
|---|---|---|
| ollama | qwen2.5:7b | 文本对话模型 |
| ollama | qwen2.5:14b | 文本对话模型（大） |
| ollama | qwen2.5vl:7b | 视觉语言模型（图片分析） |
| ollama | llama3:8b | Meta LLaMA 3 |
| ollama | mistral:7b | Mistral 7B |
| openai | gpt-4o | OpenAI GPT-4o |
| openai | gpt-4o-mini | OpenAI GPT-4o Mini |
| openai | claude-3-5-sonnet | Anthropic Claude 3.5 |
| openai | deepseek-chat | DeepSeek Chat |
| openai | deepseek-coder | DeepSeek Coder |
| openai | glm-4 | 智谱 GLM-4 |
| openai | qwen-plus | 通义千问 Plus |
| openai | ernie-bot | 文心一言 |

### 5.3 切换默认模型

```sql
-- 查看当前模型配置
SELECT id, model_name, model_type, model_id, api_url, is_default FROM ai_model_config;

-- 切换默认模型
UPDATE ai_model_config SET is_default = 0 WHERE is_default = 1;
UPDATE ai_model_config SET is_default = 1 WHERE id = <目标模型ID>;
```

### 5.4 添加新的 Ollama 模型

```bash
# 拉取新模型
ollama pull qwen2.5:14b

# 在数据库中添加配置
INSERT INTO ai_model_config (model_name, model_type, model_id, api_url, is_default, status)
VALUES ('Qwen2.5 14B', 'ollama', 'qwen2.5:14b', 'http://localhost:11434', 0, 1);
```

### 5.5 接入 OpenAI 兼容 API

```sql
INSERT INTO ai_model_config (model_name, model_type, model_id, api_url, api_key, is_default, status)
VALUES ('GPT-4o', 'openai', 'gpt-4o', 'https://api.openai.com', 'sk-your-key', 0, 1);
```

---

## 6. API 接口详解

### POST /api/ai/chat — AI 对话

**请求体：**
```json
{
  "messages": [
    { "role": "system", "content": "You are a helpful assistant..." },
    { "role": "user", "content": "你好" }
  ],
  "model": "qwen2.5:7b",
  "api_url": "http://localhost:11434",
  "model_type": "ollama"
}
```

**响应：**
```json
{ "content": "你好！有什么可以帮你的？" }
```

**字段说明：**

| 字段 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `messages` | 是 | - | 消息数组，每条包含 `role` 和 `content` |
| `model` | 否 | `qwen2.5:7b` | 模型标识 |
| `api_url` | 否 | `http://localhost:11434` | API 地址 |
| `model_type` | 否 | `ollama` | `ollama` 或 `openai` |

### POST /api/ai/analyze-image — AI 图片分析

**请求体：**
```json
{
  "image_path": "D:\\test\\photos\\2024\\01\\photo.jpg",
  "prompt": null
}
```

**响应：**
```json
{
  "tags": ["风景", "山水", "自然"],
  "city": "杭州",
  "province": "浙江",
  "description": "一张山水风景照片"
}
```

### POST /api/ai/thumbnail — 缩略图生成

**请求体：**
```json
{
  "image_path": "D:\\test\\photos\\original.jpg",
  "thumbnail_path": "D:\\test\\thumbnails\\thumb.jpg",
  "max_size": [300, 300]
}
```

### POST /api/ai/parse-exif — EXIF 解析

**请求参数：** `image_path`（query string）

**响应：**
```json
{
  "gps_lat": 30.2741,
  "gps_lng": 120.1551,
  "shoot_time": "2024:01:15 14:30:00",
  "width": 4032,
  "height": 3024
}
```

---

## 7. 环境变量与配置

### Python 服务环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama API 地址 |

### Java 服务关键配置

```yaml
# service-business/src/main/resources/application.yml
photo:
  storage:
    base-path: D:\\test\\photos
    thumbnail-path: D:\\test\\thumbnails
    video-path: D:\\test\\videos
```

### 端口分配

| 服务 | 端口 | 说明 |
|---|---|---|
| Python AI Service | 8000 | FastAPI + uvicorn |
| Java Business Service | 8081 | Spring Boot |
| Gateway | 8080 | Spring Cloud Gateway |
| Ollama | 11434 | 本地模型推理 |

---

## 8. 常见问题与调试

### Q: 图片分析返回 `["AI识别"]` 标签？

JSON 解析失败，模型返回了非 JSON 格式的内容。可能原因：
- 模型未正确加载 `qwen2.5vl:7b`
- 图片格式不支持
- 查看日志：`tail -f service-ai-analyze/ai_service.log`

### Q: AI 对话返回 HTTP 422？

Java `HttpClient` 使用了 HTTP/2 h2c 升级，uvicorn 不支持。确保：
```java
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)  // 关键！
    .build();
```

### Q: AI 对话返回 HTTP 500？

检查 Python 服务日志，常见原因：
- Ollama 未启动或模型未下载
- 模型名称与数据库配置不一致
- 请求超时（默认 120 秒）

### Q: 如何查看 Python 服务日志？

```bash
# 实时查看日志
tail -f service-ai-analyze/ai_service.log

# 或查看控制台输出（uvicorn 启动时）
```

### Q: 如何添加新的 Prompt？

1. **修改图片分析 prompt**：编辑 `service-ai-analyze/app/services/ai_client.py` 第 55 行的 `default_prompt`
2. **修改对话 system prompt**：编辑 `service-business/.../AiChatService.java` 第 42 行
3. **重启对应服务**即可生效

### Q: 如何扩展为 RAG 长期记忆？

推荐方案：
1. 引入 `chromadb` 或 `langchain` 向量数据库
2. 每次对话结束后，将消息向量化存入向量库
3. 新对话开始时，检索相关历史作为上下文注入
4. 可在 `ai_client.py` 的 `chat_completion()` 中添加检索逻辑
