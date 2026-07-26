package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yzh.stock.business.entity.FileStorage;
import yzh.stock.business.mapper.FileStorageMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private final FileStorageMapper fileMapper;

    @Value("${photo.storage.base-path:D:\\test\\photos}")
    private String basePath;

    @Value("${file.storage-dir:D:\\test\\files}")
    private String filesDir;

    public FileStorageService(FileStorageMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    public FileStorage upload(MultipartFile file, Long userId, String category, String description) throws IOException {
        String ext = getExtension(file.getOriginalFilename());
        String subDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path storageDir = Paths.get(filesDir, category != null ? category : "other", subDir);
        Files.createDirectories(storageDir);

        String fileName = UUID.randomUUID() + "." + ext;
        Path filePath = storageDir.resolve(fileName);
        file.transferTo(filePath.toFile());

        FileStorage fs = new FileStorage();
        fs.setUserId(userId);
        fs.setFileName(file.getOriginalFilename());
        fs.setStoragePath(filePath.toString());
        fs.setFileSize(file.getSize());
        fs.setFileType(ext);
        fs.setCategory(category != null ? category : "other");
        fs.setDescription(description);
        fileMapper.insert(fs);
        return fs;
    }

    public Page<FileStorage> list(Long userId, String category, int page, int size) {
        LambdaQueryWrapper<FileStorage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileStorage::getUserId, userId);
        if (category != null && !category.isEmpty()) wrapper.eq(FileStorage::getCategory, category);
        wrapper.orderByDesc(FileStorage::getCreateTime);
        return fileMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public FileStorage getById(Long id) {
        return fileMapper.selectById(id);
    }

    public void delete(Long id) {
        FileStorage fs = fileMapper.selectById(id);
        if (fs != null) {
            new java.io.File(fs.getStoragePath()).delete();
            fileMapper.deleteById(id);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "bin";
    }
}
