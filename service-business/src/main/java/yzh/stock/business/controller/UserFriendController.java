package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.stock.business.service.UserFriendService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
public class UserFriendController {

    private final UserFriendService friendService;

    public UserFriendController(UserFriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/send/{friendId}")
    public ResponseEntity<?> sendRequest(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        friendService.sendRequest(userId, friendId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已发送好友请求"));
    }

    @PostMapping("/accept/{id}")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id) {
        friendService.acceptRequest(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已接受"));
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id) {
        friendService.rejectRequest(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已拒绝"));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        friendService.removeFriend(userId, friendId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已删除好友"));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFriends(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", friendService.getFriends(userId)));
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", friendService.getPendingRequests(userId)));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam("keyword") String keyword, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", friendService.searchUsers(userId, keyword)));
    }
}
