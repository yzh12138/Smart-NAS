package yzh.nas.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yzh.nas.business.entity.UserFriend;
import yzh.nas.business.mapper.UserFriendMapper;

import java.util.List;
import java.util.Map;

@Service
public class UserFriendService {

    private final UserFriendMapper friendMapper;
    private final JdbcTemplate jdbcTemplate;

    public UserFriendService(UserFriendMapper friendMapper, JdbcTemplate jdbcTemplate) {
        this.friendMapper = friendMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void sendRequest(Long userId, Long friendId) {
        // 检查是否已存在
        UserFriend existing = friendMapper.selectOne(
                new LambdaQueryWrapper<UserFriend>()
                        .eq(UserFriend::getUserId, userId)
                        .eq(UserFriend::getFriendId, friendId)
        );
        if (existing != null) {
            if (existing.getStatus() == 2) {
                // 被拒绝后重新发送
                existing.setStatus(0);
                friendMapper.updateById(existing);
            }
            return;
        }
        // 检查反向关系
        UserFriend reverse = friendMapper.selectOne(
                new LambdaQueryWrapper<UserFriend>()
                        .eq(UserFriend::getUserId, friendId)
                        .eq(UserFriend::getFriendId, userId)
        );
        if (reverse != null) {
            if (reverse.getStatus() == 0) {
                // 对方已发来请求，直接接受
                reverse.setStatus(1);
                friendMapper.updateById(reverse);
            }
            return;
        }
        UserFriend friend = new UserFriend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(0);
        friendMapper.insert(friend);
    }

    public void acceptRequest(Long id) {
        UserFriend friend = friendMapper.selectById(id);
        if (friend != null) {
            friend.setStatus(1);
            friendMapper.updateById(friend);
        }
    }

    public void rejectRequest(Long id) {
        UserFriend friend = friendMapper.selectById(id);
        if (friend != null) {
            friend.setStatus(2);
            friendMapper.updateById(friend);
        }
    }

    public void removeFriend(Long userId, Long friendId) {
        friendMapper.delete(
                new LambdaQueryWrapper<UserFriend>()
                        .and(w -> w
                                .eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, friendId)
                                .or()
                                .eq(UserFriend::getUserId, friendId).eq(UserFriend::getFriendId, userId)
                        )
        );
    }

    public List<Map<String, Object>> getFriends(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT uf.id, uf.user_id, uf.friend_id, uf.status, uf.create_time, " +
                "u.username, u.nickname, u.avatar " +
                "FROM user_friend uf " +
                "JOIN sys_user u ON (uf.friend_id = u.id AND uf.user_id = ?) OR (uf.user_id = u.id AND uf.friend_id = ?) " +
                "WHERE ((uf.user_id = ? OR uf.friend_id = ?) AND uf.status = 1) " +
                "ORDER BY uf.create_time DESC",
                userId, userId, userId, userId
        );
    }

    public List<Map<String, Object>> getPendingRequests(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT uf.id, uf.user_id, uf.friend_id, uf.status, uf.create_time, " +
                "u.username, u.nickname " +
                "FROM user_friend uf " +
                "JOIN sys_user u ON uf.user_id = u.id " +
                "WHERE uf.friend_id = ? AND uf.status = 0 " +
                "ORDER BY uf.create_time DESC",
                userId
        );
    }

    public boolean areFriends(Long userId1, Long userId2) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_friend " +
                "WHERE ((user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)) AND status = 1",
                Long.class, userId1, userId2, userId2, userId1
        );
        return count != null && count > 0;
    }

    public List<Map<String, Object>> searchUsers(Long userId, String keyword) {
        return jdbcTemplate.queryForList(
                "SELECT id, username, nickname, avatar FROM sys_user " +
                "WHERE id != ? AND (username LIKE ? OR nickname LIKE ?) " +
                "LIMIT 20",
                userId, "%" + keyword + "%", "%" + keyword + "%"
        );
    }
}
