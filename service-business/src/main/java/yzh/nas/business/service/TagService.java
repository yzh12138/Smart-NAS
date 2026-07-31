package yzh.nas.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yzh.nas.business.entity.Tag;
import yzh.nas.business.mapper.TagMapper;

import java.util.List;
import java.util.Map;

@Service
public class TagService {

    private final TagMapper tagMapper;
    private final JdbcTemplate jdbcTemplate;

    public TagService(TagMapper tagMapper, JdbcTemplate jdbcTemplate) {
        this.tagMapper = tagMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Tag> listAll() {
        return tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getTagCategory, Tag::getTagName)
        );
    }

    public void createTag(Tag tag) {
        tagMapper.insert(tag);
    }

    public void deleteTag(Long id) {
        // 先删除关联关系
        jdbcTemplate.update("DELETE FROM photo_tag WHERE tag_id = ?", id);
        tagMapper.deleteById(id);
    }

    public long countPhotosByTag(Long tagId) {
        List<Map<String, Object>> result = jdbcTemplate.queryForList(
                "SELECT COUNT(*) as cnt FROM photo_tag WHERE tag_id = ?", tagId
        );
        if (result.isEmpty()) return 0;
        Object cnt = result.get(0).get("cnt");
        return cnt instanceof Number ? ((Number) cnt).longValue() : 0;
    }
}
