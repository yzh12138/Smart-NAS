package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.FaceCluster;
import yzh.stock.business.entity.FacePhoto;
import yzh.stock.business.mapper.FaceClusterMapper;
import yzh.stock.business.mapper.FacePhotoMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FaceClusterService {

    private final FaceClusterMapper clusterMapper;
    private final FacePhotoMapper facePhotoMapper;
    private final JdbcTemplate jdbcTemplate;

    public FaceClusterService(FaceClusterMapper clusterMapper, FacePhotoMapper facePhotoMapper, JdbcTemplate jdbcTemplate) {
        this.clusterMapper = clusterMapper;
        this.facePhotoMapper = facePhotoMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取用户的所有人脸聚类（带封面照片）
     */
    public List<Map<String, Object>> getClusters(Long userId) {
        List<FaceCluster> clusters = clusterMapper.selectList(
                new LambdaQueryWrapper<FaceCluster>()
                        .eq(FaceCluster::getUserId, userId)
                        .orderByDesc(FaceCluster::getPhotoCount)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (FaceCluster cluster : clusters) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cluster.getId());
            item.put("clusterName", cluster.getClusterName());
            item.put("photoCount", cluster.getPhotoCount());
            item.put("createTime", cluster.getCreateTime());

            // 获取该聚类的第一张照片作为封面
            List<FacePhoto> photos = facePhotoMapper.selectList(
                    new LambdaQueryWrapper<FacePhoto>()
                            .eq(FacePhoto::getClusterId, cluster.getId())
                            .last("LIMIT 1")
            );
            if (!photos.isEmpty()) {
                item.put("coverPhotoId", photos.get(0).getPhotoId());
            } else {
                item.put("coverPhotoId", null);
            }

            result.add(item);
        }
        return result;
    }

    /**
     * 获取某个聚类下的所有照片
     */
    public List<Map<String, Object>> getClusterPhotos(Long clusterId) {
        List<FacePhoto> facePhotos = facePhotoMapper.selectList(
                new LambdaQueryWrapper<FacePhoto>()
                        .eq(FacePhoto::getClusterId, clusterId)
        );

        if (facePhotos.isEmpty()) return Collections.emptyList();

        List<Long> photoIds = facePhotos.stream()
                .map(FacePhoto::getPhotoId)
                .collect(Collectors.toList());

        // 批量查询照片信息
        String placeholders = photoIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Map<String, Object>> photos = jdbcTemplate.queryForList(
                "SELECT id, original_name, storage_path, thumbnail_path, city, province, shoot_time " +
                "FROM photo WHERE id IN (" + placeholders + ") AND is_deleted = 0 ORDER BY shoot_time DESC",
                photoIds.toArray()
        );

        // 附加 face_bbox 信息
        Map<Long, String> bboxMap = facePhotos.stream()
                .collect(Collectors.toMap(FacePhoto::getPhotoId, fp -> fp.getFaceBbox() != null ? fp.getFaceBbox() : ""));

        for (Map<String, Object> photo : photos) {
            Long photoId = ((Number) photo.get("id")).longValue();
            photo.put("faceBbox", bboxMap.getOrDefault(photoId, ""));
        }

        return photos;
    }

    /**
     * 重命名聚类
     */
    public void renameCluster(Long clusterId, Long userId, String name) {
        FaceCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster != null && userId.equals(cluster.getUserId())) {
            cluster.setClusterName(name);
            clusterMapper.updateById(cluster);
        }
    }

    /**
     * 删除聚类（保留照片，只删除人脸关联）
     */
    public void deleteCluster(Long clusterId, Long userId) {
        FaceCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster != null && userId.equals(cluster.getUserId())) {
            facePhotoMapper.delete(
                    new LambdaQueryWrapper<FacePhoto>().eq(FacePhoto::getClusterId, clusterId)
            );
            clusterMapper.deleteById(clusterId);
        }
    }

    /**
     * 从聚类中移除单张照片
     */
    public void removePhotoFromCluster(Long clusterId, Long photoId) {
        facePhotoMapper.delete(
                new LambdaQueryWrapper<FacePhoto>()
                        .eq(FacePhoto::getClusterId, clusterId)
                        .eq(FacePhoto::getPhotoId, photoId)
        );
        // 更新计数
        FaceCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster != null) {
            long count = facePhotoMapper.selectCount(
                    new LambdaQueryWrapper<FacePhoto>().eq(FacePhoto::getClusterId, clusterId)
            );
            cluster.setPhotoCount((int) count);
            clusterMapper.updateById(cluster);
        }
    }

    /**
     * 将照片移动到另一个聚类
     */
    public void moveToCluster(Long photoId, Long fromClusterId, Long toClusterId) {
        FacePhoto fp = facePhotoMapper.selectOne(
                new LambdaQueryWrapper<FacePhoto>()
                        .eq(FacePhoto::getPhotoId, photoId)
                        .eq(FacePhoto::getClusterId, fromClusterId)
        );
        if (fp != null) {
            fp.setClusterId(toClusterId);
            facePhotoMapper.updateById(fp);
            // 更新两个聚类的计数
            updateClusterCount(fromClusterId);
            updateClusterCount(toClusterId);
        }
    }

    private void updateClusterCount(Long clusterId) {
        long count = facePhotoMapper.selectCount(
                new LambdaQueryWrapper<FacePhoto>().eq(FacePhoto::getClusterId, clusterId)
        );
        FaceCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster != null) {
            cluster.setPhotoCount((int) count);
            clusterMapper.updateById(cluster);
        }
    }

    /**
     * 手动创建新的空聚类
     */
    public FaceCluster createCluster(Long userId, String name) {
        FaceCluster cluster = new FaceCluster();
        cluster.setUserId(userId);
        cluster.setClusterName(name);
        cluster.setPhotoCount(0);
        clusterMapper.insert(cluster);
        return cluster;
    }

    /**
     * 将照片添加到指定聚类
     */
    public void addPhotoToCluster(Long clusterId, Long photoId) {
        // 检查是否已存在
        long exists = facePhotoMapper.selectCount(
                new LambdaQueryWrapper<FacePhoto>()
                        .eq(FacePhoto::getClusterId, clusterId)
                        .eq(FacePhoto::getPhotoId, photoId)
        );
        if (exists > 0) return;

        FacePhoto fp = new FacePhoto();
        fp.setClusterId(clusterId);
        fp.setPhotoId(photoId);
        facePhotoMapper.insert(fp);
        updateClusterCount(clusterId);
    }
}
