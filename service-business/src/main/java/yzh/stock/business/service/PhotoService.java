package yzh.stock.business.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yzh.stock.business.entity.Photo;
import yzh.stock.business.entity.Tag;
import yzh.stock.business.mapper.PhotoMapper;
import yzh.stock.business.mapper.TagMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PhotoService {

    private final PhotoMapper photoMapper;
    private final TagMapper tagMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${photo.storage.base-path:D:\\test\\photos}")
    private String storageBasePath;

    @Value("${photo.storage.thumbnail-path:D:\\test\\thumbnails}")
    private String thumbnailBasePath;

    @Value("${photo.storage.video-path:D:\\test\\videos}")
    private String videoBasePath;

    @Value("${amap.api-key:}")
    private String amapApiKey;

    private static final DateTimeFormatter FILE_NAME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss.SSS");
    private static final DateTimeFormatter SUB_DIR_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM");

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "avi", "mov", "mkv", "flv", "wmv", "webm", "3gp"
    );

    public PhotoService(PhotoMapper photoMapper, TagMapper tagMapper, JdbcTemplate jdbcTemplate) {
        this.photoMapper = photoMapper;
        this.tagMapper = tagMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 上传 ====================

    public List<Photo> uploadPhotos(MultipartFile[] files, Long userId, List<Long> tagIds,
                                     String newTags, boolean aiTag, String city, String province) throws IOException {
        List<Photo> result = new ArrayList<>();

        for (MultipartFile file : files) {
            String ext = getExtension(file.getOriginalFilename());
            boolean isVideo = VIDEO_EXTENSIONS.contains(ext);
            LocalDateTime now = LocalDateTime.now();
            String subDir = now.format(SUB_DIR_FMT);
            String timestamp = now.format(FILE_NAME_FMT);
            String fileName = timestamp + "." + ext;

            // 选择存储目录
            String baseDir = isVideo ? videoBasePath : storageBasePath;
            Path storageDir = Paths.get(baseDir, subDir);
            Files.createDirectories(storageDir);
            Path filePath = storageDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            // 生成缩略图
            String thumbFileName = "thumb_" + timestamp + ".jpg";
            Path thumbDir = Paths.get(thumbnailBasePath, subDir);
            Files.createDirectories(thumbDir);
            Path thumbPath = thumbDir.resolve(thumbFileName);

            if (isVideo) {
                generateVideoThumbnail(filePath, thumbPath);
            } else {
                generateThumbnail(filePath, thumbPath);
            }

            // 解析 EXIF 数据（仅图片）
            ExifData exif = new ExifData();
            if (!isVideo) {
                exif = parseExif(filePath.toFile());
                // 读取图片宽高
                try {
                    BufferedImage img = ImageIO.read(filePath.toFile());
                    if (img != null) {
                        exif.width = img.getWidth();
                        exif.height = img.getHeight();
                    }
                } catch (Exception ignored) {}
            }

            Photo photo = new Photo();
            photo.setUserId(userId);
            photo.setOriginalName(file.getOriginalFilename());
            photo.setStoragePath(filePath.toString());
            photo.setThumbnailPath(thumbPath.toString());
            photo.setFileSize(file.getSize());
            photo.setMimeType(file.getContentType());
            photo.setMediaType(isVideo ? "video" : "image");
            photo.setFileHash(computeHash(filePath));
            photo.setWidth(exif.width);
            photo.setHeight(exif.height);
            photo.setGpsLat(exif.lat);
            photo.setGpsLng(exif.lng);
            photo.setShootTime(exif.shootTime);
            photo.setAiAnalyzed(0);
            photo.setCreateTime(now);
            photoMapper.insert(photo);

            // GPS 反编码获取城市（手动地址优先）
            if (city != null && !city.isEmpty()) {
                photo.setCity(city);
                photo.setProvince(province);
                photoMapper.updateById(photo);
            } else if (exif.lat != null && exif.lng != null && (photo.getCity() == null)) {
                try {
                    Map<String, String> geo = reverseGeocode(exif.lat, exif.lng);
                    photo.setCity(geo.get("city"));
                    photo.setProvince(geo.get("province"));
                    photoMapper.updateById(photo);
                } catch (Exception e) {
                    // 反编码失败不影响上传
                }
            }

            // 手动标签
            if (tagIds != null && !tagIds.isEmpty()) {
                for (Long tagId : tagIds) {
                    insertPhotoTag(photo.getId(), tagId, 1);
                }
            }

            // 新建标签
            if (newTags != null && !newTags.isEmpty()) {
                for (String tagName : newTags.split("[,，]")) {
                    tagName = tagName.trim();
                    if (!tagName.isEmpty()) {
                        Tag tag = getOrCreateTag(tagName, null);
                        insertPhotoTag(photo.getId(), tag.getId(), 1);
                    }
                }
            }

            // AI 标签分析（同步调用 Python 服务）
            if (aiTag && !isVideo) {
                try {
                    callAiAnalyze(photo.getId(), filePath.toString());
                } catch (Exception e) {
                    // AI 分析失败不影响上传
                }
            }

            result.add(photo);
        }

        return result;
    }

    // ==================== AI 标签选择流程 ====================

    /**
     * 获取 AI 建议标签（不保存，返回给前端让用户选择）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAiSuggestedTags(Long photoId) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null) {
            throw new RuntimeException("照片不存在");
        }

        try {
            String json = "{\"image_path\":\"" + photo.getStoragePath().replace("\\", "\\\\") + "\"}";
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8000/api/ai/analyze-image"))
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> aiResult = mapper.readValue(response.body(), Map.class);
                // 如果AI识别到水印经纬度且照片没有GPS数据，返回给前端
                Object watermarkLatObj = aiResult.get("watermark_lat");
                Object watermarkLngObj = aiResult.get("watermark_lng");
                if (watermarkLatObj != null && watermarkLngObj != null && photo.getGpsLat() == null) {
                    aiResult.put("watermark_lat", watermarkLatObj);
                    aiResult.put("watermark_lng", watermarkLngObj);
                } else {
                    aiResult.remove("watermark_lat");
                    aiResult.remove("watermark_lng");
                }
                return aiResult;
            }
        } catch (Exception e) {
            // AI 分析失败
        }

        Map<String, Object> fallback = new HashMap<>();
        fallback.put("tags", Collections.emptyList());
        fallback.put("city", null);
        fallback.put("province", null);
        return fallback;
    }

    /**
     * 用户确认选择的标签，保存到数据库
     */
    public void confirmAiTags(Long photoId, List<String> selectedTags, String city, String province) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null) return;

        // 保存用户选择的标签
        if (selectedTags != null) {
            for (String tagName : selectedTags) {
                tagName = tagName.trim();
                if (!tagName.isEmpty()) {
                    Tag tag = getOrCreateTag(tagName, null);
                    insertPhotoTag(photoId, tag.getId(), 2); // 2=AI建议用户确认
                }
            }
        }

        // 更新城市信息
        if (city != null) photo.setCity(city);
        if (province != null) photo.setProvince(province);
        photo.setAiAnalyzed(1);
        photoMapper.updateById(photo);
    }

    // ==================== 查询 ====================

    public List<Photo> getSharedPhotos(Long userId) {
        List<Map<String, Object>> sharedList = jdbcTemplate.queryForList(
                "SELECT p.id, p.user_id, p.original_name, p.storage_path, p.thumbnail_path, " +
                "p.file_size, p.mime_type, p.width, p.height, p.city, p.province, p.media_type, p.create_time, fm.shared_by " +
                "FROM photo p " +
                "JOIN family_media fm ON p.id = fm.photo_id " +
                "JOIN family_member fam ON fm.family_id = fam.family_id " +
                "WHERE fam.user_id = ? AND fam.status = 1 " +
                "AND p.is_deleted = 0 " +
                "ORDER BY p.create_time DESC",
                userId
        );
        List<Photo> photos = new ArrayList<>();
        for (Map<String, Object> row : sharedList) {
            Photo p = new Photo();
            p.setId(((Number) row.get("id")).longValue());
            p.setUserId(((Number) row.get("user_id")).longValue());
            p.setOriginalName((String) row.get("original_name"));
            p.setStoragePath((String) row.get("storage_path"));
            p.setThumbnailPath((String) row.get("thumbnail_path"));
            p.setFileSize(row.get("file_size") != null ? ((Number) row.get("file_size")).longValue() : null);
            p.setMimeType((String) row.get("mime_type"));
            p.setWidth(row.get("width") != null ? ((Number) row.get("width")).intValue() : null);
            p.setHeight(row.get("height") != null ? ((Number) row.get("height")).intValue() : null);
            p.setCity((String) row.get("city"));
            p.setProvince((String) row.get("province"));
            p.setMediaType((String) row.get("media_type"));
            p.setCreateTime(row.get("create_time") != null ? ((java.sql.Timestamp) row.get("create_time")).toLocalDateTime() : null);
            photos.add(p);
        }
        return photos;
    }

    // ==================== AI 单张扫描 ====================

    public Map<String, Object> scanSinglePhoto(Long userId, Long photoId) {
        Photo photo = photoMapper.selectOne(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getId, photoId)
                        .eq(Photo::getUserId, userId)
                        .eq(Photo::getIsDeleted, 0)
        );
        Map<String, Object> result = new HashMap<>();
        if (photo == null) {
            result.put("success", false);
            result.put("message", "照片不存在");
            return result;
        }
        try {
            callAiAnalyze(photo.getId(), photo.getStoragePath());
            result.put("success", true);
            result.put("photoId", photoId);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== AI 批量扫描 ====================

    public Map<String, Object> batchAiScan(Long userId, List<Long> photoIds) {
        List<Photo> photos;
        if (photoIds != null && !photoIds.isEmpty()) {
            // 指定照片扫描
            photos = photoMapper.selectList(
                    new LambdaQueryWrapper<Photo>()
                            .in(Photo::getId, photoIds)
                            .eq(Photo::getUserId, userId)
                            .eq(Photo::getIsDeleted, 0)
                            .eq(Photo::getMediaType, "image")
            );
        } else {
            // 全量扫描：所有未分析的照片
            photos = photoMapper.selectList(
                    new LambdaQueryWrapper<Photo>()
                            .eq(Photo::getUserId, userId)
                            .eq(Photo::getIsDeleted, 0)
                            .eq(Photo::getAiAnalyzed, 0)
                            .eq(Photo::getMediaType, "image")
                            .orderByDesc(Photo::getCreateTime)
            );
        }
        int total = photos.size();
        int success = 0;
        int failed = 0;

        for (Photo photo : photos) {
            try {
                callAiAnalyze(photo.getId(), photo.getStoragePath());
                success++;
            } catch (Exception e) {
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        return result;
    }

    public List<Map<String, Object>> getAiReviewQueue(Long userId) {
        List<Map<String, Object>> queue = jdbcTemplate.queryForList(
                "SELECT p.id, p.original_name, p.city, p.province, p.gps_lat, p.gps_lng, p.ai_analyzed, p.create_time, " +
                "GROUP_CONCAT(t.tag_name) as ai_tag_names " +
                "FROM photo p " +
                "LEFT JOIN photo_tag pt ON p.id = pt.photo_id AND pt.tag_source = 2 " +
                "LEFT JOIN tag t ON pt.tag_id = t.id " +
                "WHERE p.user_id = ? AND p.is_deleted = 0 AND p.media_type = 'image' " +
                "AND (p.ai_analyzed = 1 OR NOT EXISTS (SELECT 1 FROM photo_tag pt2 WHERE pt2.photo_id = p.id)) " +
                "GROUP BY p.id, p.original_name, p.city, p.province, p.gps_lat, p.gps_lng, p.ai_analyzed, p.create_time " +
                "ORDER BY p.ai_analyzed ASC, p.create_time DESC",
                userId
        );
        return queue;
    }

    public void reviewAiTags(Long photoId, List<String> approvedTags, String city, String province, boolean approved) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null) return;

        if (approved && approvedTags != null) {
            // 清除之前的AI标签
            jdbcTemplate.update("DELETE FROM photo_tag WHERE photo_id = ? AND tag_source = 2", photoId);
            for (String tagName : approvedTags) {
                tagName = tagName.trim();
                if (!tagName.isEmpty()) {
                    Tag tag = getOrCreateTag(tagName, null);
                    insertPhotoTag(photoId, tag.getId(), 2);
                }
            }
        } else if (!approved) {
            // 拒绝：清除AI标签
            jdbcTemplate.update("DELETE FROM photo_tag WHERE photo_id = ? AND tag_source = 2", photoId);
        }

        if (city != null) photo.setCity(city);
        if (province != null) photo.setProvince(province);
        photoMapper.updateById(photo);
    }

    public Page<Photo> listPhotos(int page, int size, Long userId, Long tagId, String city, String startDate, String endDate, String mediaType) {
        LambdaQueryWrapper<Photo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Photo::getUserId, userId);
        wrapper.eq(Photo::getIsDeleted, 0);
        // 按媒体类型过滤：image / video / 不指定则返回所有
        if (mediaType != null && !mediaType.isEmpty()) {
            wrapper.eq(Photo::getMediaType, mediaType);
        }
        if (city != null && !city.isEmpty()) {
            wrapper.eq(Photo::getCity, city);
        }
        if (tagId != null) {
            wrapper.inSql(Photo::getId, "SELECT photo_id FROM photo_tag WHERE tag_id = " + tagId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Photo::getCreateTime, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Photo::getCreateTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(Photo::getCreateTime);
        return photoMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<Photo> searchPhotos(Long userId, String keyword) {
        // 先查询匹配关键词的标签ID（使用参数化查询，防止SQL注入）
        List<Long> tagIds = tagMapper.selectObjs(
                new LambdaQueryWrapper<Tag>()
                        .like(Tag::getTagName, keyword)
                        .select(Tag::getId)
        );
        
        LambdaQueryWrapper<Photo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Photo::getUserId, userId);
        wrapper.eq(Photo::getIsDeleted, 0);
        wrapper.and(w -> w
                .like(Photo::getOriginalName, keyword)
                .or().like(Photo::getCity, keyword)
                .or().like(Photo::getProvince, keyword)
        );
        // 如果有匹配的标签，添加标签筛选条件
        if (!tagIds.isEmpty()) {
            wrapper.or().in(Photo::getId, 
                photoMapper.selectObjs(
                    new LambdaQueryWrapper<Photo>()
                        .select(Photo::getId)
                        .apply("SELECT photo_id FROM photo_tag WHERE tag_id IN ({0})", 
                               tagIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
                )
            );
        }
        wrapper.orderByDesc(Photo::getCreateTime);
        return photoMapper.selectList(wrapper);
    }

    public Photo getPhotoById(Long id) {
        Photo photo = photoMapper.selectById(id);
        if (photo != null && photo.getIsDeleted() != null && photo.getIsDeleted() == 1) {
            return null;
        }
        return photo;
    }

    public Photo getPhotoByIdAny(Long id) {
        return photoMapper.selectById(id);
    }

    // ==================== 点击计数 ====================

    public void incrementClickCount(Long photoId) {
        jdbcTemplate.update("UPDATE photo SET click_count = click_count + 1 WHERE id = ?", photoId);
    }

    public List<Photo> getRecommendedPhotos(Long userId) {
        return photoMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getUserId, userId)
                        .eq(Photo::getIsDeleted, 0)
                        .gt(Photo::getClickCount, 5)
                        .orderByDesc(Photo::getClickCount)
                        .last("LIMIT 20")
        );
    }

    public List<Tag> getPhotoTags(Long photoId) {
        return tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .inSql(Tag::getId, "SELECT tag_id FROM photo_tag WHERE photo_id = " + photoId)
        );
    }

    public void deletePhoto(Long id) {
        // 移入回收站而非直接删除
        Photo photo = photoMapper.selectById(id);
        if (photo != null) {
            photo.setIsDeleted(1);
            photo.setDeletedTime(LocalDateTime.now());
            photoMapper.updateById(photo);
        }
    }

    public void updatePhotoName(Long id, String newName) {
        Photo photo = photoMapper.selectById(id);
        if (photo != null) {
            photo.setOriginalName(newName);
            photoMapper.updateById(photo);
        }
    }

    public List<Map<String, Object>> getAllUserStats() {
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                "SELECT u.id, u.username, u.nickname, " +
                "COUNT(p.id) as photo_count, " +
                "SUM(CASE WHEN p.media_type='video' THEN 1 ELSE 0 END) as video_count, " +
                "SUM(p.file_size) as total_size " +
                "FROM sys_user u LEFT JOIN photo p ON u.id = p.user_id AND p.is_deleted = 0 " +
                "GROUP BY u.id, u.username, u.nickname ORDER BY photo_count DESC"
        );
        return stats;
    }

    public List<Map<String, Object>> getCityPhotoStats(Long userId) {
        List<Photo> photos = photoMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getUserId, userId)
                        .eq(Photo::getIsDeleted, 0)
                        .isNotNull(Photo::getCity)
                        .select(Photo::getCity, Photo::getId)
        );
        Map<String, Long> cityCount = new HashMap<>();
        for (Photo p : photos) {
            cityCount.merge(p.getCity(), 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        cityCount.forEach((city, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("city", city);
            item.put("count", count);
            result.add(item);
        });
        return result;
    }

    public List<Photo> getPhotosByCity(Long userId, String city) {
        return photoMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getUserId, userId)
                        .eq(Photo::getCity, city)
                        .eq(Photo::getIsDeleted, 0)
                        .orderByDesc(Photo::getCreateTime)
        );
    }

    public void updatePhotoAiResult(Long photoId, String city, String province,
                                     BigDecimal lat, BigDecimal lng) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo != null) {
            photo.setAiAnalyzed(1);
            if (city != null) photo.setCity(city);
            if (province != null) photo.setProvince(province);
            if (lat != null) photo.setGpsLat(lat);
            if (lng != null) photo.setGpsLng(lng);
            photoMapper.updateById(photo);
        }
    }

    // ==================== 标签 ====================

    public List<Tag> listTags() {
        return tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getTagCategory, Tag::getTagName)
        );
    }

    private Tag getOrCreateTag(String tagName, String category) {
        Tag existing = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>().eq(Tag::getTagName, tagName)
        );
        if (existing != null) return existing;
        Tag tag = new Tag();
        tag.setTagName(tagName);
        tag.setTagCategory(category != null ? category : "other");
        tagMapper.insert(tag);
        return tag;
    }

    private void insertPhotoTag(Long photoId, Long tagId, int source) {
        try {
            jdbcTemplate.update(
                    "INSERT IGNORE INTO photo_tag (photo_id, tag_id, tag_source) VALUES (?, ?, ?)",
                    photoId, tagId, source
            );
        } catch (Exception e) {
            // ignore
        }
    }

    // ==================== 工具方法 ====================

    private String computeHash(Path filePath) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(filePath);
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }

    private void generateThumbnail(Path source, Path target) {
        try {
            BufferedImage original = ImageIO.read(source.toFile());
            if (original == null) return;
            int w = original.getWidth();
            int h = original.getHeight();
            int maxSize = 300;
            if (w <= maxSize && h <= maxSize) {
                ImageIO.write(original, "jpg", target.toFile());
                return;
            }
            double scale = Math.min((double) maxSize / w, (double) maxSize / h);
            int newW = (int) (w * scale);
            int newH = (int) (h * scale);
            BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            thumb.getGraphics().drawImage(
                    original.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            ImageIO.write(thumb, "jpg", target.toFile());
        } catch (Exception e) {
            // 缩略图生成失败不影响主流程
        }
    }

    private void generateVideoThumbnail(Path videoPath, Path target) {
        // 视频缩略图：尝试用 Java 原生方式，失败则创建占位图
        try {
            BufferedImage placeholder = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = placeholder.createGraphics();
            g.setColor(java.awt.Color.DARK_GRAY);
            g.fillRect(0, 0, 300, 200);
            g.setColor(java.awt.Color.WHITE);
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
            g.drawString("VIDEO", 110, 108);
            g.dispose();
            ImageIO.write(placeholder, "jpg", target.toFile());
        } catch (Exception e) {
            // ignore
        }
    }

    private static class ExifData {
        BigDecimal lat;
        BigDecimal lng;
        LocalDateTime shootTime;
        Integer width;
        Integer height;
    }

    private ExifData parseExif(File file) {
        ExifData data = new ExifData();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                data.lat = BigDecimal.valueOf(gpsDir.getGeoLocation().getLatitude());
                data.lng = BigDecimal.valueOf(gpsDir.getGeoLocation().getLongitude());
            }
            ExifSubIFDDirectory exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDir != null) {
                Date date = exifDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    data.shootTime = date.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            }
        } catch (Exception e) {
            // EXIF 解析失败不影响上传
        }
        return data;
    }

    // ==================== GPS 反向编码（高德 API） ====================

    @SuppressWarnings("unchecked")
    public Map<String, String> reverseGeocode(BigDecimal lat, BigDecimal lng) {
        Map<String, String> result = new HashMap<>();
        if (amapApiKey == null || amapApiKey.isEmpty()) return result;

        try {
            String url = String.format(
                    "https://restapi.amap.com/v3/geocode/regeo?key=%s&location=%s,%s&extensions=base",
                    amapApiKey, lng, lat
            );
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> geoResult = mapper.readValue(response.body(), Map.class);
                if ("1".equals(String.valueOf(geoResult.get("status")))) {
                    Map<String, Object> regeocode = (Map<String, Object>) geoResult.get("regeocode");
                    if (regeocode != null) {
                        Map<String, Object> addressComponent = (Map<String, Object>) regeocode.get("addressComponent");
                        if (addressComponent != null) {
                            result.put("city", (String) addressComponent.get("city"));
                            result.put("province", (String) addressComponent.get("province"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 反编码失败
        }
        return result;
    }

    // ==================== AI 分析（内部调用） ====================

    @SuppressWarnings("unchecked")
    private void callAiAnalyze(Long photoId, String imagePath) {
        try {
            String json = "{\"image_path\":\"" + imagePath.replace("\\", "\\\\") + "\"}";
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8000/api/ai/analyze-image"))
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> aiResult = mapper.readValue(response.body(), Map.class);
                List<String> tags = (List<String>) aiResult.get("tags");
                if (tags != null) {
                    for (String tagName : tags) {
                        Tag tag = getOrCreateTag(tagName, null);
                        insertPhotoTag(photoId, tag.getId(), 2);
                    }
                }
                Photo photo = photoMapper.selectById(photoId);
                if (photo != null) {
                    String city = (String) aiResult.get("city");
                    String province = (String) aiResult.get("province");
                    if (city != null && !city.isEmpty()) photo.setCity(city);
                    if (province != null && !province.isEmpty()) photo.setProvince(province);
                    // 使用水印中的经纬度（如果EXIF中没有GPS数据）
                    Object watermarkLatObj = aiResult.get("watermark_lat");
                    Object watermarkLngObj = aiResult.get("watermark_lng");
                    if (watermarkLatObj != null && watermarkLngObj != null) {
                        try {
                            double watermarkLat = Double.parseDouble(String.valueOf(watermarkLatObj));
                            double watermarkLng = Double.parseDouble(String.valueOf(watermarkLngObj));
                            if (photo.getGpsLat() == null) photo.setGpsLat(BigDecimal.valueOf(watermarkLat));
                            if (photo.getGpsLng() == null) photo.setGpsLng(BigDecimal.valueOf(watermarkLng));
                        } catch (NumberFormatException ignored) {}
                    }
                    photo.setAiAnalyzed(1);
                    photoMapper.updateById(photo);
                }
            }
        } catch (Exception e) {
            // AI 分析失败不影响上传
        }
    }
}
