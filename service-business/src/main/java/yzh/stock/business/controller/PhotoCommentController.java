package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.stock.business.entity.PhotoComment;
import yzh.stock.business.service.PhotoCommentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photo/{photoId}/comment")
public class PhotoCommentController {

    private final PhotoCommentService commentService;

    public PhotoCommentController(PhotoCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<?> getComments(@PathVariable Long photoId) {
        List<Map<String, Object>> comments = commentService.getComments(photoId);
        return ResponseEntity.ok(Map.of("code", 200, "data", comments));
    }

    @PostMapping
    public ResponseEntity<?> addComment(@PathVariable Long photoId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        PhotoComment comment = commentService.addComment(photoId, userId, body.get("content"));
        return ResponseEntity.ok(Map.of("code", 200, "data", comment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long photoId, @PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        commentService.deleteComment(id, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已删除"));
    }
}
