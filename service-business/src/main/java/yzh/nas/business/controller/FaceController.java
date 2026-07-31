package yzh.nas.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.nas.business.entity.FaceCluster;
import yzh.nas.business.service.FaceClusterService;

import java.util.Map;

@RestController
@RequestMapping("/api/face")
public class FaceController {

    private final FaceClusterService faceService;

    public FaceController(FaceClusterService faceService) {
        this.faceService = faceService;
    }

    @GetMapping("/clusters")
    public ResponseEntity<?> getClusters(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", faceService.getClusters(userId)));
    }

    @GetMapping("/cluster/{id}/photos")
    public ResponseEntity<?> getClusterPhotos(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", faceService.getClusterPhotos(id)));
    }

    @PostMapping("/cluster")
    public ResponseEntity<?> createCluster(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        String name = body.getOrDefault("name", "未命名人物");
        FaceCluster cluster = faceService.createCluster(userId, name);
        return ResponseEntity.ok(Map.of("code", 200, "data", cluster));
    }

    @PutMapping("/cluster/{id}")
    public ResponseEntity<?> renameCluster(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        faceService.renameCluster(id, userId, body.get("name"));
        return ResponseEntity.ok(Map.of("code", 200, "message", "已更新"));
    }

    @DeleteMapping("/cluster/{id}")
    public ResponseEntity<?> deleteCluster(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        faceService.deleteCluster(id, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已删除"));
    }

    @DeleteMapping("/cluster/{clusterId}/photo/{photoId}")
    public ResponseEntity<?> removePhoto(@PathVariable Long clusterId, @PathVariable Long photoId) {
        faceService.removePhotoFromCluster(clusterId, photoId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已移除"));
    }

    @PostMapping("/cluster/{toClusterId}/photo/{photoId}")
    public ResponseEntity<?> moveToCluster(@PathVariable Long toClusterId, @PathVariable Long photoId,
                                            @RequestParam(required = false) Long fromClusterId) {
        if (fromClusterId != null) {
            faceService.moveToCluster(photoId, fromClusterId, toClusterId);
        } else {
            faceService.addPhotoToCluster(toClusterId, photoId);
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "已移动"));
    }
}
