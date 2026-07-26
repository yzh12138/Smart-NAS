package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yzh.stock.business.entity.Photo;
import yzh.stock.business.service.PhotoService;
import yzh.stock.business.service.LogService;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/photo")
public class PhotoController {

    private final PhotoService photoService;
    private final LogService logService;

    public PhotoController(PhotoService photoService, LogService logService) {
        this.photoService = photoService;
        this.logService = logService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPhotos(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
            @RequestParam(value = "newTags", required = false) String newTags,
            @RequestParam(value = "aiTag", defaultValue = "false") boolean aiTag,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "province", required = false) String province,
            HttpServletRequest request) {
        try {
            Long userId = Long.parseLong(request.getHeader("X-User-Id"));
            String username = request.getHeader("X-Username");
            List<Photo> photos = photoService.uploadPhotos(files, userId, tagIds, newTags, aiTag, city, province);
            logService.log(userId, username, "上传照片", "photo", null, "上传了 " + photos.size() + " 张照片/视频", request.getRemoteAddr());
            return ResponseEntity.ok(Map.of("code", 200, "message", "上传成功", "data", photos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 500, "message", "上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listPhotos(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "mediaType", required = false) String mediaType,
            HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data",
                photoService.listPhotos(page, size, userId, tagId, city, startDate, endDate, mediaType)));
    }

    @GetMapping("/{id}/thumb")
    public ResponseEntity<Resource> getThumbnail(@PathVariable Long id) {
        Photo photo = photoService.getPhotoByIdAny(id);
        if (photo == null || photo.getThumbnailPath() == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(photo.getThumbnailPath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(new FileSystemResource(file));
    }

    @GetMapping("/{id}/original")
    public ResponseEntity<Resource> getOriginal(@PathVariable Long id) {
        Photo photo = photoService.getPhotoById(id);
        if (photo == null || photo.getStoragePath() == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(photo.getStoragePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        String contentType = photo.getMimeType() != null ? photo.getMimeType() : "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getOriginalName() + "\"")
                .body(new FileSystemResource(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPhoto(@PathVariable Long id) {
        Photo photo = photoService.getPhotoById(id);
        if (photo == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "照片不存在"));
        }
        var tags = photoService.getPhotoTags(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", photo, "tags", tags));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        String username = request.getHeader("X-Username");
        photoService.deletePhoto(id);
        logService.log(userId, username, "删除照片", "photo", id, "移入回收站", request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("code", 200, "message", "已移入回收站"));
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<?> updateName(@PathVariable Long id, @RequestBody Map<String, String> body) {
        photoService.updatePhotoName(id, body.get("name"));
        return ResponseEntity.ok(Map.of("code", 200, "message", "名称已更新"));
    }

    @GetMapping("/map/cities")
    public ResponseEntity<?> getCityStats(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", photoService.getCityPhotoStats(userId)));
    }

    @GetMapping("/map/city/{city}")
    public ResponseEntity<?> getPhotosByCity(@PathVariable String city, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", photoService.getPhotosByCity(userId, city)));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPhotos(
            @RequestParam(value = "keyword") String keyword,
            HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", photoService.searchPhotos(userId, keyword)));
    }

    @PostMapping("/{id}/ai-tags")
    public ResponseEntity<?> getAiSuggestedTags(@PathVariable Long id) {
        try {
            Map<String, Object> result = photoService.getAiSuggestedTags(id);
            return ResponseEntity.ok(Map.of("code", 200, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirm-tags")
    public ResponseEntity<?> confirmAiTags(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.get("tags");
        String city = (String) body.get("city");
        String province = (String) body.get("province");
        photoService.confirmAiTags(id, tags, city, province);
        return ResponseEntity.ok(Map.of("code", 200, "message", "标签已保存"));
    }

    @GetMapping("/shared")
    public ResponseEntity<?> getSharedPhotos(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", photoService.getSharedPhotos(userId)));
    }

    @PostMapping("/batch-ai-scan")
    public ResponseEntity<?> batchAiScan(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        List<Long> photoIds = null;
        if (body != null && body.containsKey("photoIds")) {
            @SuppressWarnings("unchecked")
            List<Number> nums = (List<Number>) body.get("photoIds");
            if (nums != null) {
                photoIds = new ArrayList<>();
                for (Number n : nums) photoIds.add(n.longValue());
            }
        }
        Map<String, Object> result = photoService.batchAiScan(userId, photoIds);
        return ResponseEntity.ok(Map.of("code", 200, "data", result));
    }

    @PostMapping("/scan-single/{id}")
    public ResponseEntity<?> scanSinglePhoto(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        Map<String, Object> result = photoService.scanSinglePhoto(userId, id);
        return ResponseEntity.ok(Map.of("code", 200, "data", result));
    }

    @GetMapping("/ai-review")
    public ResponseEntity<?> getAiReviewQueue(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", photoService.getAiReviewQueue(userId)));
    }

    @PostMapping("/{id}/ai-review")
    public ResponseEntity<?> reviewAiTags(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        @SuppressWarnings("unchecked")
        List<String> approvedTags = (List<String>) body.get("tags");
        String city = (String) body.get("city");
        String province = (String) body.get("province");
        Boolean approved = (Boolean) body.get("approved");
        photoService.reviewAiTags(id, approvedTags, city, province, approved != null ? approved : true);
        return ResponseEntity.ok(Map.of("code", 200, "message", "审核完成"));
    }

    @GetMapping("/user-stats")
    public ResponseEntity<?> getUserStats(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        String username = request.getHeader("X-Username");
        // 仅管理员可查看所有用户统计
        if (!"admin".equals(username)) {
            return ResponseEntity.ok(Map.of("code", 403, "message", "仅管理员可查看"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", photoService.getAllUserStats()));
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<?> reverseGeocode(
            @RequestParam("lat") java.math.BigDecimal lat,
            @RequestParam("lng") java.math.BigDecimal lng) {
        Map<String, String> result = photoService.reverseGeocode(lat, lng);
        return ResponseEntity.ok(Map.of("code", 200, "data", result));
    }

    @PostMapping("/{id}/click")
    public ResponseEntity<?> trackClick(@PathVariable Long id) {
        photoService.incrementClickCount(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "ok"));
    }

    @GetMapping("/recommended")
    public ResponseEntity<?> getRecommendedPhotos(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", photoService.getRecommendedPhotos(userId)));
    }
}
