package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yzh.stock.business.entity.Book;
import yzh.stock.business.service.BookService;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/book")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "title", required = false) String title,
                                     @RequestParam(value = "author", required = false) String author,
                                     @RequestParam(value = "category", required = false) String category,
                                     @RequestParam(value = "tags", required = false) String tags,
                                     @RequestParam(value = "visibility", required = false) String visibility,
                                     HttpServletRequest request) {
        try {
            Long userId = Long.parseLong(request.getHeader("X-User-Id"));
            Book book = bookService.upload(file, userId, title, author, category, tags, visibility);
            return ResponseEntity.ok(Map.of("code", 200, "data", book));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 500, "message", e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String format,
                                   @RequestParam(required = false) String visibility,
                                   HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data",
                bookService.search(userId, keyword, category, format, visibility, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Book book = bookService.getById(id);
        if (book == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "图书不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", book));
    }

    @GetMapping("/{id}/read")
    public ResponseEntity<Resource> read(@PathVariable Long id, HttpServletRequest request) {
        Book book = bookService.getById(id);
        if (book == null) return ResponseEntity.notFound().build();
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        // 公共图书可读，私有图书需验证所有权
        if (!"public".equals(book.getVisibility()) && !userId.equals(book.getUserId())) {
            return ResponseEntity.status(403).build();
        }
        File file = new File(book.getStoragePath());
        if (!file.exists()) return ResponseEntity.notFound().build();
        String format = book.getFileFormat() != null ? book.getFileFormat().toLowerCase() : "";
        String contentType = switch (format) {
            case "pdf" -> "application/pdf";
            case "epub" -> "application/epub+zip";
            default -> "application/octet-stream";
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + book.getFileName() + "\"")
                .body(new FileSystemResource(file));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        Book book = bookService.getById(id);
        if (book == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "图书不存在"));
        }
        if (!userId.equals(book.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权修改此图书"));
        }
        bookService.update(id, body.get("title"), body.get("author"), body.get("tags"), body.get("visibility"));
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        Book book = bookService.getById(id);
        if (book == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "图书不存在"));
        }
        if (!userId.equals(book.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("code", 403, "message", "无权删除此图书"));
        }
        bookService.delete(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已删除"));
    }
}
