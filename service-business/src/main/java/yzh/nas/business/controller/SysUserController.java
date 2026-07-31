package yzh.nas.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.nas.business.entity.SysUser;
import yzh.nas.business.service.SysUserService;
import yzh.nas.business.utils.PasswordUtil;

import java.util.Map;

@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> listUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        var result = userService.listUsers(page, size);
        return ResponseEntity.ok(Map.of("code", 200, "data", result));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody SysUser user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
        }
        userService.createUser(user);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        // 如果提供了新密码，哈希后保存
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
        } else {
            // 没有提供密码，不更新密码字段
            user.setPassword(null);
        }
        userService.updateUser(user);
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @GetMapping("/ai-prompt")
    public ResponseEntity<?> getAiPrompt(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        SysUser user = userService.getById(userId);
        String aiPrompt = user != null ? user.getAiPrompt() : null;
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("aiPrompt", aiPrompt != null ? aiPrompt : "");
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @PutMapping("/ai-prompt")
    public ResponseEntity<?> updateAiPrompt(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        SysUser user = userService.getById(userId);
        if (user != null) {
            user.setAiPrompt(body.get("aiPrompt"));
            userService.updateUser(user);
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "提示词已更新"));
    }
}
