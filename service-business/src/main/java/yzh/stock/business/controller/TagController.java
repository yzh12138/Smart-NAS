package yzh.stock.business.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.stock.business.entity.Tag;
import yzh.stock.business.service.TagService;

import java.util.Map;

@RestController
@RequestMapping("/api/tag")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> listTags() {
        return ResponseEntity.ok(Map.of("code", 200, "data", tagService.listAll()));
    }

    @PostMapping
    public ResponseEntity<?> createTag(@RequestBody Tag tag) {
        tagService.createTag(tag);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功"));
    }

    @GetMapping("/{id}/photo-count")
    public ResponseEntity<?> getPhotoCount(@PathVariable Long id) {
        long count = tagService.countPhotosByTag(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", count));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id) {
        long photoCount = tagService.countPhotosByTag(id);
        if (photoCount > 0) {
            return ResponseEntity.ok(Map.of("code", 400, "message", "该标签已关联 " + photoCount + " 张照片，无法直接删除。请先移除照片上的此标签。"));
        }
        tagService.deleteTag(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
