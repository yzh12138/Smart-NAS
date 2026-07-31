package yzh.nas.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import yzh.nas.business.entity.AiConversation;
import yzh.nas.business.entity.AiMessage;
import yzh.nas.business.entity.AiModelConfig;
import yzh.nas.business.entity.SysUser;
import yzh.nas.business.mapper.AiConversationMapper;
import yzh.nas.business.mapper.AiMessageMapper;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class AiChatService {

    private final AiConversationMapper convMapper;
    private final AiMessageMapper msgMapper;
    private final AiModelService modelService;
    private final SysUserService userService;

    @Value("${ai.service-url:http://127.0.0.1:8000}")
    private String aiServiceUrl;

    @Value("${ai.config-dir:D:\\test}")
    private String configDir;

    @Value("${ai.workspace-dir:D:\\test\\workspace}")
    private String workspaceDir;

    public AiChatService(AiConversationMapper convMapper, AiMessageMapper msgMapper, AiModelService modelService, SysUserService userService) {
        this.convMapper = convMapper;
        this.msgMapper = msgMapper;
        this.modelService = modelService;
        this.userService = userService;
    }

    public boolean isConversationOwner(Long conversationId, Long userId) {
        AiConversation conv = convMapper.selectById(conversationId);
        return conv != null && userId.equals(conv.getUserId());
    }

    public AiConversation createConversation(Long userId, String title, Long modelConfigId) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setTitle(title != null ? title : "新对话");
        conv.setModelConfigId(modelConfigId);
        conv.setSystemPrompt(getUserPrompt(userId));
        String workspacePath = Paths.get(workspaceDir, String.valueOf(userId), "conv_" + System.currentTimeMillis()).toString();
        try { Files.createDirectories(Paths.get(workspacePath)); } catch (Exception e) { /* ignore */ }
        conv.setWorkspacePath(workspacePath);
        convMapper.insert(conv);
        return conv;
    }

    private String readGlobalPrompt() {
        try {
            Path path = Paths.get(configDir, "ai_prompt.json");
            if (Files.exists(path)) {
                return Files.readString(path).trim();
            }
        } catch (Exception e) { /* ignore */ }
        return "You are a helpful assistant for a photo management NAS system.";
    }

    private String getUserPrompt(Long userId) {
        SysUser user = userService.getById(userId);
        if (user != null && user.getAiPrompt() != null && !user.getAiPrompt().isEmpty()) {
            return user.getAiPrompt();
        }
        return readGlobalPrompt();
    }

    public List<AiConversation> getConversations(Long userId) {
        return convMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdateTime)
        );
    }

    public List<AiMessage> getMessages(Long conversationId) {
        return msgMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreateTime)
        );
    }

    @SuppressWarnings("unchecked")
    public AiMessage sendMessage(Long conversationId, String content) {
        return sendMessage(conversationId, content, null);
    }

    @SuppressWarnings("unchecked")
    public AiMessage sendMessage(Long conversationId, String content, String imagePath) {
        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setImagePath(imagePath);
        msgMapper.insert(userMsg);

        AiConversation conv = convMapper.selectById(conversationId);
        if (conv == null) {
            throw new IllegalArgumentException("对话不存在: " + conversationId);
        }
        // 使用对话指定的模型，如果没有则使用默认模型
        AiModelConfig model = null;
        if (conv.getModelConfigId() != null) {
            model = modelService.getById(conv.getModelConfigId());
        }
        if (model == null) {
            model = modelService.getDefault();
        }

        // 构建 system prompt（优先级：对话记忆 > 用户提示词 > 全局提示词）
        // 模型 promptTemplate 作为前缀附加
        String conversationPrompt = conv.getSystemPrompt();
        String userPrompt = getUserPrompt(conv.getUserId());
        String globalPrompt = readGlobalPrompt();

        String systemPrompt;
        if (conversationPrompt != null && !conversationPrompt.isEmpty()) {
            // 对话级记忆优先
            systemPrompt = conversationPrompt;
        } else if (userPrompt != null && !userPrompt.isEmpty()) {
            // 用户级提示词次之
            systemPrompt = userPrompt;
        } else {
            // 全局提示词兜底
            systemPrompt = globalPrompt;
        }

        // 如果模型有 promptTemplate，附加在 system prompt 之前
        if (model != null && model.getPromptTemplate() != null && !model.getPromptTemplate().isEmpty()) {
            systemPrompt = model.getPromptTemplate() + "\n\n" + systemPrompt;
        }

        String aiResponse;
        try {
            List<AiMessage> history = getMessages(conversationId);
            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, Object> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
            }
            boolean hasImages = false;
            for (AiMessage m : history) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("role", m.getRole() != null ? m.getRole() : "user");
                msgMap.put("content", m.getContent() != null ? m.getContent() : "");
                if (m.getImagePath() != null && !m.getImagePath().isEmpty()) {
                    try {
                        byte[] imageBytes = java.nio.file.Files.readAllBytes(Paths.get(m.getImagePath()));
                        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                        msgMap.put("images", List.of(base64));
                        hasImages = true;
                    } catch (Exception e) { /* ignore unreadable images */ }
                }
                messages.add(msgMap);
            }

            String modelId = model != null && model.getModelId() != null ? model.getModelId() : "qwen2.5:7b";
            if (hasImages && !modelId.contains("vl")) {
                modelId = "qwen2.5vl:7b";
            }

            Map<String, Object> body = new HashMap<>();
            body.put("messages", messages);
            body.put("model", modelId);
            body.put("api_url", model != null && model.getApiUrl() != null ? model.getApiUrl() : "http://localhost:11434");
            body.put("model_type", model != null && model.getModelType() != null ? model.getModelType() : "ollama");
            if (model != null && model.getApiKey() != null && !model.getApiKey().isEmpty()) {
                body.put("api_key", model.getApiKey());
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            String jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
            // 记录请求时隐藏敏感信息（api_key）
            Map<String, Object> logBody = new HashMap<>(body);
            if (logBody.containsKey("api_key")) {
                logBody.put("api_key", "***");
            }
            System.out.println("[AI Chat] Request body: " + mapper.writeValueAsString(logBody));

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/api/ai/chat"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(java.time.Duration.ofSeconds(120))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = mapper.readValue(response.body(), Map.class);
                aiResponse = (String) result.get("content");
            } else {
                System.err.println("[AI Chat] Python service returned HTTP " + response.statusCode() + ": " + response.body());
                aiResponse = "AI 服务返回错误 (HTTP " + response.statusCode() + "): " + response.body();
            }
        } catch (java.net.ConnectException e) {
            aiResponse = "无法连接到 AI 服务，请确认 Python AI 服务已启动 (port 8000)。";
        } catch (Exception e) {
            System.err.println("[AI Chat] Error: " + e.getMessage());
            e.printStackTrace();
            aiResponse = "AI 调用失败: " + e.getMessage();
        }

        if (aiResponse == null) aiResponse = "AI 未返回有效回复。";

        AiMessage aiMsg = new AiMessage();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResponse);
        msgMapper.insert(aiMsg);

        if ("新对话".equals(conv.getTitle()) && !content.isEmpty()) {
            conv.setTitle(content.substring(0, Math.min(content.length(), 50)));
            convMapper.updateById(conv);
        }

        return aiMsg;
    }

    public void deleteConversation(Long id) {
        msgMapper.delete(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getConversationId, id));
        convMapper.deleteById(id);
    }

    public void updateConversationModel(Long conversationId, Long modelConfigId) {
        AiConversation conv = convMapper.selectById(conversationId);
        if (conv != null) {
            conv.setModelConfigId(modelConfigId);
            convMapper.updateById(conv);
        }
    }

    public void updateConversationTitle(Long conversationId, String title) {
        AiConversation conv = convMapper.selectById(conversationId);
        if (conv != null && title != null && !title.isEmpty()) {
            conv.setTitle(title);
            convMapper.updateById(conv);
        }
    }

    public void updateConversation(Long conversationId, String title, String systemPrompt) {
        AiConversation conv = convMapper.selectById(conversationId);
        if (conv != null) {
            if (title != null && !title.isEmpty()) conv.setTitle(title);
            if (systemPrompt != null) conv.setSystemPrompt(systemPrompt);
            convMapper.updateById(conv);
        }
    }
}
