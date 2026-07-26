package yzh.stock.business.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.stock.business.entity.Family;
import yzh.stock.business.service.FamilyService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @PostMapping
    public ResponseEntity<?> createFamily(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        Family family = familyService.createFamily(body.get("name"), userId, body.get("description"));
        return ResponseEntity.ok(Map.of("code", 200, "data", family));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFamily(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        if (!familyService.isAdminOrOwner(id, userId)) {
            return ResponseEntity.badRequest().body(Map.of("code", 403, "message", "仅管理员和创建者可以编辑家庭"));
        }
        familyService.updateFamily(id, body.get("name"), body.get("description"));
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> dissolveFamily(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        if (!familyService.isAdminOrOwner(id, userId)) {
            return ResponseEntity.badRequest().body(Map.of("code", 403, "message", "仅管理员和创建者可以解散家庭"));
        }
        familyService.dissolveFamily(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已解散"));
    }

    @GetMapping("/my")
    public ResponseEntity<?> myFamilies(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", familyService.getUserFamilies(userId)));
    }

    @GetMapping("/owned")
    public ResponseEntity<?> ownedFamilies(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(Map.of("code", 200, "data", familyService.getOwnedFamilies(userId)));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinFamily(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        familyService.addMember(id, userId, "member", 0);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已申请，等待审批"));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<?> inviteMember(@PathVariable Long id, @RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        if (!familyService.isAdminOrOwner(id, userId)) {
            return ResponseEntity.badRequest().body(Map.of("code", 403, "message", "仅管理员和创建者可以邀请成员"));
        }
        Long friendId = body.get("userId");
        if (friendId == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "缺少用户ID"));
        }
        familyService.addMember(id, friendId, "member", 1);
        return ResponseEntity.ok(Map.of("code", 200, "message", "邀请成功"));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", familyService.getMembers(id)));
    }

    @GetMapping("/{id}/pending")
    public ResponseEntity<?> getPending(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", familyService.getPendingMembers(id)));
    }

    @PostMapping("/member/{memberId}/approve")
    public ResponseEntity<?> approveMember(@PathVariable Long memberId) {
        familyService.approveMember(memberId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已通过"));
    }

    @PostMapping("/member/{memberId}/reject")
    public ResponseEntity<?> rejectMember(@PathVariable Long memberId) {
        familyService.rejectMember(memberId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已拒绝"));
    }

    @DeleteMapping("/{familyId}/member/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long familyId, @PathVariable Long userId) {
        familyService.removeMember(familyId, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已移除"));
    }

    @PostMapping("/{familyId}/share/{photoId}")
    public ResponseEntity<?> shareMedia(@PathVariable Long familyId, @PathVariable Long photoId, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        familyService.shareMedia(familyId, photoId, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已共享"));
    }

    @PostMapping("/{familyId}/batch-share")
    public ResponseEntity<?> batchShareMedia(@PathVariable Long familyId, @RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        List<Long> photoIds = body.get("photoIds");
        if (photoIds != null) {
            for (Long photoId : photoIds) {
                familyService.shareMedia(familyId, photoId, userId);
            }
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "已共享 " + (photoIds != null ? photoIds.size() : 0) + " 张照片"));
    }

    @DeleteMapping("/{familyId}/unshare/{photoId}")
    public ResponseEntity<?> unshareMedia(@PathVariable Long familyId, @PathVariable Long photoId) {
        familyService.unshareMedia(familyId, photoId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已取消共享"));
    }

    @PostMapping("/batch-unshare")
    public ResponseEntity<?> batchUnshare(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        @SuppressWarnings("unchecked")
        List<Number> photoIds = (List<Number>) body.get("photoIds");
        if (photoIds != null) {
            for (Number photoId : photoIds) {
                familyService.unshareFromAllFamilies(userId, photoId.longValue());
            }
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "已取消共享"));
    }

    @GetMapping("/{id}/media")
    public ResponseEntity<?> getFamilyMedia(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", familyService.getFamilyMedia(id)));
    }

    @GetMapping("/search/{code}")
    public ResponseEntity<?> searchByCode(@PathVariable String code) {
        Family family = familyService.findByCode(code);
        if (family == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "未找到该家庭"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", family));
    }
}
