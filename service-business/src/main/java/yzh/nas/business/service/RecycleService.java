package yzh.nas.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yzh.nas.business.entity.Photo;
import yzh.nas.business.mapper.PhotoMapper;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecycleService {

    private final PhotoMapper photoMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${photo.storage.base-path:D:\\test\\photos}")
    private String storageBasePath;

    @Value("${photo.storage.thumbnail-path:D:\\test\\thumbnails}")
    private String thumbnailBasePath;

    @Value("${photo.storage.video-path:D:\\test\\videos}")
    private String videoBasePath;

    public RecycleService(PhotoMapper photoMapper, JdbcTemplate jdbcTemplate) {
        this.photoMapper = photoMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void moveToRecycle(Long photoId) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo != null) {
            photo.setIsDeleted(1);
            photo.setDeletedTime(LocalDateTime.now());
            photoMapper.updateById(photo);
        }
    }

    public void restore(Long photoId) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo != null) {
            photo.setIsDeleted(0);
            photo.setDeletedTime(null);
            photoMapper.updateById(photo);
        }
    }

    public void permanentDelete(Long photoId) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo != null) {
            // 删除文件
            deleteFile(photo.getStoragePath());
            deleteFile(photo.getThumbnailPath());
            // 删除关联表数据
            jdbcTemplate.update("DELETE FROM photo_tag WHERE photo_id = ?", photoId);
            jdbcTemplate.update("DELETE FROM photo_comment WHERE photo_id = ?", photoId);
            jdbcTemplate.update("DELETE FROM family_media WHERE photo_id = ?", photoId);
            // 删除数据库记录
            photoMapper.deleteById(photoId);
        }
    }

    public void emptyRecycle(Long userId) {
        List<Photo> photos = photoMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getUserId, userId)
                        .eq(Photo::getIsDeleted, 1)
        );
        for (Photo photo : photos) {
            permanentDelete(photo.getId());
        }
    }

    public Page<Photo> getRecycleList(Long userId, int page, int size) {
        return photoMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getUserId, userId)
                        .eq(Photo::getIsDeleted, 1)
                        .orderByDesc(Photo::getDeletedTime)
        );
    }

    public void cleanExpiredRecycle() {
        List<Photo> photos = photoMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getIsDeleted, 1)
                        .isNotNull(Photo::getDeletedTime)
        );
        for (Photo photo : photos) {
            if (photo.getDeletedTime() != null && photo.getRecycleDays() != null) {
                LocalDateTime expireTime = photo.getDeletedTime().plusDays(photo.getRecycleDays());
                if (LocalDateTime.now().isAfter(expireTime)) {
                    permanentDelete(photo.getId());
                }
            }
        }
    }

    private void deleteFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists()) file.delete();
        }
    }
}
