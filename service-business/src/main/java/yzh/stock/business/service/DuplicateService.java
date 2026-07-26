package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.Photo;
import yzh.stock.business.mapper.PhotoMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DuplicateService {

    private final PhotoMapper photoMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RecycleService recycleService;

    public DuplicateService(PhotoMapper photoMapper, JdbcTemplate jdbcTemplate, RecycleService recycleService) {
        this.photoMapper = photoMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.recycleService = recycleService;
    }

    public List<Map<String, Object>> findDuplicates(Long userId) {
        List<Photo> photos = photoMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getUserId, userId)
                        .eq(Photo::getIsDeleted, 0)
                        .isNotNull(Photo::getFileHash)
        );

        Map<String, List<Photo>> grouped = photos.stream()
                .filter(p -> p.getFileHash() != null)
                .collect(Collectors.groupingBy(Photo::getFileHash));

        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((hash, list) -> {
            if (list.size() > 1) {
                Map<String, Object> group = new HashMap<>();
                group.put("hash", hash);
                group.put("count", list.size());
                group.put("photos", list);
                result.add(group);
            }
        });
        return result;
    }

    public int cleanDuplicates(Long userId, List<Long> keepIds) {
        List<Map<String, Object>> duplicates = findDuplicates(userId);
        int count = 0;
        for (Map<String, Object> group : duplicates) {
            @SuppressWarnings("unchecked")
            List<Photo> photos = (List<Photo>) group.get("photos");
            for (Photo p : photos) {
                if (!keepIds.contains(p.getId())) {
                    recycleService.moveToRecycle(p.getId());
                    count++;
                }
            }
        }
        return count;
    }
}
