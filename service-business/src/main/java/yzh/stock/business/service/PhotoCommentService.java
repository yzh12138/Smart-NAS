package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.PhotoComment;
import yzh.stock.business.mapper.PhotoCommentMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PhotoCommentService {

    private final PhotoCommentMapper commentMapper;
    private final JdbcTemplate jdbcTemplate;

    public PhotoCommentService(PhotoCommentMapper commentMapper, JdbcTemplate jdbcTemplate) {
        this.commentMapper = commentMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getComments(Long photoId) {
        return jdbcTemplate.queryForList(
                "SELECT c.id, c.photo_id, c.user_id, c.content, c.create_time, u.username, u.nickname " +
                "FROM photo_comment c " +
                "LEFT JOIN sys_user u ON c.user_id = u.id " +
                "WHERE c.photo_id = ? " +
                "ORDER BY c.create_time ASC",
                photoId
        );
    }

    public PhotoComment addComment(Long photoId, Long userId, String content) {
        PhotoComment comment = new PhotoComment();
        comment.setPhotoId(photoId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);
        return comment;
    }

    public void deleteComment(Long id, Long userId) {
        commentMapper.delete(
                new LambdaQueryWrapper<PhotoComment>()
                        .eq(PhotoComment::getId, id)
                        .eq(PhotoComment::getUserId, userId)
        );
    }
}
