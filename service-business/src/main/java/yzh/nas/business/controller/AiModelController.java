package yzh.nas.business.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.nas.business.entity.AiModelConfig;
import yzh.nas.business.service.AiModelService;

import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-model")
public class AiModelController {

    private final AiModelService aiModelService;

    @Value("${ai.config-dir:D:\\test}")
    private String configDir;

    public AiModelController(AiModelService aiModelService) {
        this.aiModelService = aiModelService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(Map.of("code", 200, "data", aiModelService.listAll()));
    }

    @GetMapping("/default")
    public ResponseEntity<?> getDefault() {
        AiModelConfig config = aiModelService.getDefault();
        return ResponseEntity.ok(Map.of("code", 200, "data", config != null ? config : Map.of()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", aiModelService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AiModelConfig config) {
        aiModelService.create(config);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AiModelConfig config) {
        config.setId(id);
        aiModelService.update(config);
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        aiModelService.delete(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<?> setDefault(@PathVariable Long id) {
        aiModelService.setDefault(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已设为默认"));
    }

    @GetMapping("/global-prompt")
    public ResponseEntity<?> getGlobalPrompt() {
        try {
            Path path = Paths.get(configDir, "ai_prompt.json");
            if (Files.exists(path)) {
                String content = Files.readString(path);
                return ResponseEntity.ok(Map.of("code", 200, "data", Map.of("prompt", content)));
            }
        } catch (Exception e) { /* ignore */ }
        return ResponseEntity.ok(Map.of("code", 200, "data", Map.of("prompt", "You are a helpful assistant for a photo management NAS system.")));
    }

    @PutMapping("/global-prompt")
    public ResponseEntity<?> updateGlobalPrompt(@RequestBody Map<String, String> body) {
        try {
            String prompt = body.getOrDefault("prompt", "");
            Path dir = Paths.get(configDir);
            Files.createDirectories(dir);
            Files.writeString(Paths.get(configDir, "ai_prompt.json"), prompt);
            return ResponseEntity.ok(Map.of("code", 200, "message", "全局提示词已更新"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 500, "message", "保存失败: " + e.getMessage()));
        }
    }
}
