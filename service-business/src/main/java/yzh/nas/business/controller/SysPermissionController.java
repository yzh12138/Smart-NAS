package yzh.nas.business.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.nas.business.entity.SysPermission;
import yzh.nas.business.service.SysPermissionService;

import java.util.Map;

@RestController
@RequestMapping("/api/system/permission")
public class SysPermissionController {

    private final SysPermissionService permissionService;

    public SysPermissionController(SysPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/tree")
    public ResponseEntity<?> getPermissionTree() {
        return ResponseEntity.ok(Map.of("code", 200, "data", permissionService.getPermissionTree()));
    }

    @GetMapping("/list")
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(Map.of("code", 200, "data", permissionService.listAll()));
    }

    @PostMapping
    public ResponseEntity<?> createPermission(@RequestBody SysPermission perm) {
        permissionService.createPermission(perm);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePermission(@PathVariable Long id, @RequestBody SysPermission perm) {
        perm.setId(id);
        permissionService.updatePermission(perm);
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
