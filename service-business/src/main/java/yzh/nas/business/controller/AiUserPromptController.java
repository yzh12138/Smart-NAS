package yzh.nas.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.nas.business.entity.AiUserPrompt;
import yzh.nas.business.service.AiUserPromptService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-prompt")
public class AiUserPromptController {

    private final AiUserPromptService promptService;

    public AiUserPromptController(AiUserPromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> listPrompts(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        List<AiUserPrompt> prompts = promptService.listPrompts(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", prompts));
    }

    @PostMapping
    public ResponseEntity<?> createPrompt(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        AiUserPrompt prompt = promptService.createPrompt(userId, body.get("name"), body.get("content"));
        return ResponseEntity.ok(Map.of("code", 200, "data", prompt));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePrompt(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        AiUserPrompt prompt = promptService.updatePrompt(id, userId, body.get("name"), body.get("content"));
        return ResponseEntity.ok(Map.of("code", 200, "data", prompt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePrompt(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        promptService.deletePrompt(id, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已删除"));
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<?> setDefault(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        promptService.setDefault(id, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已设为默认"));
    }

    @GetMapping("/default")
    public ResponseEntity<?> getDefault(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        AiUserPrompt prompt = promptService.getDefault(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", prompt));
    }
}
