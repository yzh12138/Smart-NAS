package yzh.nas.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yzh.nas.business.entity.Book;
import yzh.nas.business.mapper.BookMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class BookService {

    private final BookMapper bookMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${book.storage-dir:D:\\test\\books}")
    private String booksDir;

    public BookService(BookMapper bookMapper, JdbcTemplate jdbcTemplate) {
        this.bookMapper = bookMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Book upload(MultipartFile file, Long userId, String title, String author,
                       String category, String tags, String visibility) throws IOException {
        String ext = getExtension(file.getOriginalFilename());
        Path storageDir = Paths.get(booksDir, String.valueOf(userId));
        Files.createDirectories(storageDir);

        String fileName = UUID.randomUUID() + "." + ext;
        Path filePath = storageDir.resolve(fileName);
        file.transferTo(filePath.toFile());

        Book book = new Book();
        book.setUserId(userId);
        book.setTitle(title != null ? title : file.getOriginalFilename());
        book.setAuthor(author);
        book.setCategory(category != null ? category : "other");
        book.setTags(tags);
        book.setFileName(file.getOriginalFilename());
        book.setStoragePath(filePath.toString());
        book.setFileSize(file.getSize());
        book.setFileFormat(ext);
        book.setVisibility(visibility != null ? visibility : "private");
        bookMapper.insert(book);
        return book;
    }

    public Page<Book> search(Long userId, String keyword, String category, String format,
                              String visibility, int page, int size) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        // 公共书库 + 自己的书
        wrapper.and(w -> w
                .eq(Book::getVisibility, "public")
                .or().eq(Book::getUserId, userId)
        );
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getTags, keyword)
            );
        }
        if (category != null && !category.isEmpty()) wrapper.eq(Book::getCategory, category);
        if (format != null && !format.isEmpty()) wrapper.eq(Book::getFileFormat, format);
        if (visibility != null && !visibility.isEmpty()) wrapper.eq(Book::getVisibility, visibility);
        wrapper.orderByDesc(Book::getCreateTime);
        return bookMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Book getById(Long id) {
        return bookMapper.selectById(id);
    }

    public void delete(Long id) {
        Book book = bookMapper.selectById(id);
        if (book != null) {
            new java.io.File(book.getStoragePath()).delete();
            bookMapper.deleteById(id);
        }
    }

    public void update(Long id, String title, String author, String tags, String visibility) {
        Book book = bookMapper.selectById(id);
        if (book != null) {
            if (title != null) book.setTitle(title);
            if (author != null) book.setAuthor(author);
            if (tags != null) book.setTags(tags);
            if (visibility != null) book.setVisibility(visibility);
            bookMapper.updateById(book);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "pdf";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "pdf";
    }
}
