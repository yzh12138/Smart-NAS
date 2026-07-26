package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.stock.business.service.DuplicateService;

import java.util.Map;

@RestController
@RequestMapping("/api/duplicate")
public class DuplicateController {

    private final DuplicateService duplicateService;

    public DuplicateController(DuplicateService duplicateService) {
        this.duplicateService = duplicateService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", duplicateService.findDuplicates(userId)));
    }

    @PostMapping("/clean")
    public ResponseEntity<?> clean(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        @SuppressWarnings("unchecked")
        java.util.List<?> rawIds = (java.util.List<?>) body.get("keepIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "keepIds不能为空"));
        }
        java.util.List<Long> keepIds = rawIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();
        int count = duplicateService.cleanDuplicates(userId, keepIds);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已移入回收站 " + count + " 个重复文件"));
    }
}
