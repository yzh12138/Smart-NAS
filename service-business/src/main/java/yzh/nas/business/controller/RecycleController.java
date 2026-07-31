package yzh.nas.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.nas.business.entity.Photo;
import yzh.nas.business.service.PhotoService;
import yzh.nas.business.service.RecycleService;

import java.util.Map;

@RestController
@RequestMapping("/api/recycle")
public class RecycleController {

    private final RecycleService recycleService;
    private final PhotoService photoService;

    public RecycleController(RecycleService recycleService, PhotoService photoService) {
        this.recycleService = recycleService;
        this.photoService = photoService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", recycleService.getRecycleList(userId, page, size)));
    }

    @PostMapping("/restore/{id}")
    public ResponseEntity<?> restore(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        Photo photo = photoService.getPhotoByIdAny(id);
        if (photo == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "照片不存在"));
        }
        if (!userId.equals(photo.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权恢复此照片"));
        }
        recycleService.restore(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已恢复"));
    }

    @DeleteMapping("/permanent/{id}")
    public ResponseEntity<?> permanentDelete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        Photo photo = photoService.getPhotoByIdAny(id);
        if (photo == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "照片不存在"));
        }
        if (!userId.equals(photo.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权删除此照片"));
        }
        recycleService.permanentDelete(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已永久删除"));
    }

    @DeleteMapping("/empty")
    public ResponseEntity<?> empty(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        recycleService.emptyRecycle(userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "回收站已清空"));
    }
}
