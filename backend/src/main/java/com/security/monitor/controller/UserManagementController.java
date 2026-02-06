package com.security.monitor.controller;

import com.security.monitor.dto.CreateUserRequest;
import com.security.monitor.dto.UpdateUserRequest;
import com.security.monitor.dto.UserDTO;
import com.security.monitor.service.UserManagementService;
import com.security.monitor.service.SystemLogService;
import com.security.monitor.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEVELOPER')")
public class UserManagementController {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 获取用户列表（分页）
     * 开发者可以看到所有用户，管理员和超级管理员看不到开发者账号
     */
    @GetMapping
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 检查当前用户是否是开发者
        boolean isDeveloper = isDeveloperRole();
        Page<UserDTO> users = userManagementService.getAllUsers(pageable, isDeveloper);
        return ResponseEntity.ok(users);
    }

    /**
     * 检查当前用户是否是开发者角色
     */
    private boolean isDeveloperRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DEVELOPER"));
        }
        return false;
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userManagementService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建新用户
     */
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            UserDTO user = userManagementService.createUser(request);
            // 记录用户创建日志
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String ipAddress = RequestUtil.getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            systemLogService.logUserManagement(null, auth.getName(),
                    "创建用户", request.getUsername(),
                    String.format("角色: %s, 邮箱: %s", request.getRole(), request.getEmail()),
                    ipAddress, userAgent);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserDTO user = userManagementService.updateUser(id, request);
            // 记录用户更新日志
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String ipAddress = RequestUtil.getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            systemLogService.logUserManagement(null, auth.getName(),
                    "更新用户", user.getUsername(),
                    String.format("用户ID: %d, 邮箱: %s", id, user.getEmail()),
                    ipAddress, userAgent);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            // 先获取用户信息用于日志记录
            UserDTO userToDelete = userManagementService.getUserById(id).orElse(null);
            userManagementService.deleteUser(id);
            // 记录用户删除日志
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String ipAddress = RequestUtil.getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            systemLogService.logUserManagement(null, auth.getName(),
                    "删除用户", userToDelete != null ? userToDelete.getUsername() : "ID:" + id,
                    String.format("用户ID: %d, 角色: %s", id, userToDelete != null ? userToDelete.getRole() : "未知"),
                    ipAddress, userAgent);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 修改用户角色
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String role = request.get("role");
            if (role == null || !role.matches("USER|ADMIN|SUPER_ADMIN|DEVELOPER")) {
                return ResponseEntity.badRequest().build();
            }
            // 先获取旧角色
            UserDTO oldUser = userManagementService.getUserById(id).orElse(null);
            String oldRole = oldUser != null ? oldUser.getRole() : "未知";

            UserDTO user = userManagementService.updateUserRole(id, role);
            // 记录角色变更日志
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String ipAddress = RequestUtil.getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            systemLogService.logUserManagement(null, auth.getName(),
                    "修改用户角色", user.getUsername(),
                    String.format("用户ID: %d, 角色变更: %s -> %s", id, oldRole, role),
                    ipAddress, userAgent);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
