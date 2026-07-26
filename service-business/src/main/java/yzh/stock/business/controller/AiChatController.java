package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yzh.stock.business.entity.AiConversation;
import yzh.stock.business.entity.AiMessage;
import yzh.stock.business.service.AiChatService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-chat")
public class AiChatController {

    private final AiChatService chatService;

    @Value("${ai.chat-image-dir:D:\\test\\chat_images}")
    private String chatImageDir;

    public AiChatController(AiChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversation")
    public ResponseEntity<?> createConversation(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        Long modelConfigId = body.get("modelConfigId") != null ? Long.valueOf(body.get("modelConfigId").toString()) : null;
        AiConversation conv = chatService.createConversation(userId, (String) body.get("title"), modelConfigId);
        return ResponseEntity.ok(Map.of("code", 200, "data", conv));
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", chatService.getConversations(userId)));
    }

    @GetMapping("/conversation/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        if (!chatService.isConversationOwner(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权访问此对话"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", chatService.getMessages(id)));
    }

    @PostMapping("/conversation/{id}/send")
    public ResponseEntity<?> send(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        if (!chatService.isConversationOwner(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权访问此对话"));
        }
        try {
            AiMessage reply = chatService.sendMessage(id, body.get("content"), body.get("imagePath"));
            return ResponseEntity.ok(Map.of("code", 200, "data", reply));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 500, "message", "AI调用失败: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String ext = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                    : ".jpg";
            String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")) + ext;
            Path dir = Paths.get(chatImageDir);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(fileName);
            file.transferTo(filePath.toFile());
            return ResponseEntity.ok(Map.of("code", 200, "data", "chat_images/" + fileName));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 500, "message", "上传失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/conversation/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        if (!chatService.isConversationOwner(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权删除此对话"));
        }
        chatService.deleteConversation(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已删除"));
    }

    @PutMapping("/conversation/{id}/model")
    public ResponseEntity<?> updateConversationModel(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        if (!chatService.isConversationOwner(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权修改此对话"));
        }
        Long modelConfigId = body.get("modelConfigId") != null ? Long.valueOf(body.get("modelConfigId").toString()) : null;
        chatService.updateConversationModel(id, modelConfigId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "模型已更新"));
    }

    @PutMapping("/conversation/{id}")
    public ResponseEntity<?> updateConversation(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        if (!chatService.isConversationOwner(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权修改此对话"));
        }
        String title = body.get("title");
        String systemPrompt = body.get("systemPrompt");
        chatService.updateConversation(id, title, systemPrompt);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已更新"));
    }
}
