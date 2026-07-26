package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yzh.stock.business.entity.FileStorage;
import yzh.stock.business.service.FileStorageService;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
public class FileStorageController {

    private final FileStorageService fileService;
    private final JdbcTemplate jdbcTemplate;

    public FileStorageController(FileStorageService fileService, JdbcTemplate jdbcTemplate) {
        this.fileService = fileService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "category", required = false) String category,
                                     @RequestParam(value = "description", required = false) String description,
                                     HttpServletRequest request) {
        try {
            Long userId = Long.parseLong(request.getHeader("X-User-Id"));
            FileStorage fs = fileService.upload(file, userId, category, description);
            return ResponseEntity.ok(Map.of("code", 200, "data", fs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String category,
                                   HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", fileService.list(userId, category, page, size)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileStorage fs = fileService.getById(id);
        if (fs == null) return ResponseEntity.notFound().build();
        File file = new File(fs.getStoragePath());
        if (!file.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fs.getFileName() + "\"")
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        FileStorage fs = fileService.getById(id);
        if (fs == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "文件不存在"));
        }
        if (!userId.equals(fs.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权删除此文件"));
        }
        fileService.delete(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已删除"));
    }

    @Value("${photo.storage.base-path:D:\\test\\photos}")
    private String storageBasePath;

    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            Long userId = Long.parseLong(request.getHeader("X-User-Id"));
            String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + ".jpg";
            java.nio.file.Path avatarDir = java.nio.file.Paths.get(storageBasePath, "avatars");
            java.nio.file.Files.createDirectories(avatarDir);
            java.nio.file.Path avatarPath = avatarDir.resolve(fileName);
            file.transferTo(avatarPath.toFile());
            String avatarUrl = "/api/file/avatar/" + fileName;
            jdbcTemplate.update("UPDATE sys_user SET avatar = ? WHERE id = ?", avatarUrl, userId);
            return ResponseEntity.ok(Map.of("code", 200, "data", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    @GetMapping("/avatar/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        java.nio.file.Path avatarPath = java.nio.file.Paths.get(storageBasePath, "avatars", filename);
        File file = avatarPath.toFile();
        if (!file.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new FileSystemResource(file));
    }
}
