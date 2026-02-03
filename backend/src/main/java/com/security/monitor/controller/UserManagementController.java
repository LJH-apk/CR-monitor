package com.security.monitor.controller;

import com.security.monitor.dto.CreateUserRequest;
import com.security.monitor.dto.UpdateUserRequest;
import com.security.monitor.dto.UserDTO;
import com.security.monitor.service.UserManagementService;
import com.security.monitor.service.SystemLogService;
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
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserDTO user = userManagementService.createUser(request);
            // 记录用户创建日志
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            systemLogService.logUserManagement(null, auth.getName(),
                    "创建用户", request.getUsername(),
                    String.format("角色: %s, 邮箱: %s", request.getRole(), request.getEmail()));
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
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserDTO user = userManagementService.updateUser(id, request);
            // 记录用户更新日志
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            systemLogService.logUserManagement(null, auth.getName(),
                    "更新用户", user.getUsername(),
                    String.format("用户ID: %d", id));
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            // 先获取用户信息用于日志记录
            UserDTO userToDelete = userManagementService.getUserById(id).orElse(null);
            userManagementService.deleteUser(id);
            // 记录用户删除日志
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            systemLogService.logUserManagement(null, auth.getName(),
                    "删除用户", userToDelete != null ? userToDelete.getUsername() : "ID:" + id,
                    String.format("用户ID: %d", id));
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
            @RequestBody Map<String, String> request) {
        try {
            String role = request.get("role");
            if (role == null || !role.matches("USER|ADMIN|SUPER_ADMIN|DEVELOPER")) {
                return ResponseEntity.badRequest().build();
            }
            UserDTO user = userManagementService.updateUserRole(id, role);
            // 记录角色变更日志
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            systemLogService.logUserManagement(null, auth.getName(),
                    "修改用户角色", user.getUsername(),
                    String.format("用户ID: %d, 新角色: %s", id, role));
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
