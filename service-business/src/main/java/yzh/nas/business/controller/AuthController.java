package yzh.nas.business.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.nas.business.entity.SysPermission;
import yzh.nas.business.entity.SysRole;
import yzh.nas.business.entity.SysUser;
import yzh.nas.business.service.SysPermissionService;
import yzh.nas.business.service.SysRoleService;
import yzh.nas.business.service.SysUserService;
import yzh.nas.business.service.LogService;
import yzh.nas.business.utils.PasswordUtil;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysPermissionService permissionService;
    private final StringRedisTemplate redisTemplate;
    private final LogService logService;

    @Value("${jwt.secret:smart-nas-secret-key-must-be-at-least-32-bytes-long!!}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public AuthController(SysUserService userService, SysRoleService roleService,
                          SysPermissionService permissionService, StringRedisTemplate redisTemplate, LogService logService) {
        this.userService = userService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.redisTemplate = redisTemplate;
        this.logService = logService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        SysUser user = userService.findByUsername(username);
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "用户名或密码错误"));
        }
        if (user.getStatus() != 1) {
            String msg = user.getStatus() == 0 ? "账号待审批，请等待管理员审核" : "账号已禁用";
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", msg));
        }

        String token = generateToken(user);
        redisTemplate.opsForValue().set("token:" + user.getId(), token, jwtExpiration, TimeUnit.MILLISECONDS);
        logService.log(user.getId(), user.getUsername(), "用户登录", "user", user.getId(), "登录成功", null);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());

        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "未登录"));
        }

        SysUser user = userService.getById(Long.parseLong(userId));
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "用户不存在"));
        }

        List<SysRole> roles = roleService.getRolesByUserId(user.getId());
        List<SysPermission> permissions = permissionService.getPermissionsByUserId(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("familyRole", user.getFamilyRole());
        data.put("roles", roles);
        data.put("permissions", permissions);

        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        if (userId != null) {
            redisTemplate.delete("token:" + userId);
            logService.log(Long.parseLong(userId), username, "用户登出", "user", Long.parseLong(userId), "退出登录", null);
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "已退出"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "用户名不能为空"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "密码不能为空"));
        }

        SysUser existing = userService.findByUsername(username.trim());
        if (existing != null) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "用户名已存在"));
        }

        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setPassword(PasswordUtil.hashPassword(password));
        user.setNickname(nickname != null ? nickname.trim() : username.trim());
        user.setStatus(0); // 待审批
        userService.createUser(user);

        return ResponseEntity.ok(Map.of("code", 200, "message", "注册成功，等待管理员审批"));
    }

    private String generateToken(SysUser user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }
}
