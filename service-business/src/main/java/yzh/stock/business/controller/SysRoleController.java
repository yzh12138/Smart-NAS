package yzh.stock.business.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.stock.business.entity.SysRole;
import yzh.stock.business.service.SysRoleService;

import java.util.Map;

@RestController
@RequestMapping("/api/system/role")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> listRoles() {
        return ResponseEntity.ok(Map.of("code", 200, "data", roleService.listAll()));
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody SysRole role) {
        roleService.createRole(role);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.updateRole(role);
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
