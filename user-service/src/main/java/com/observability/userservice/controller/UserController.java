package com.observability.userservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.observability.userservice.dto.UserDTO;
import com.observability.userservice.service.UserService;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    private final Tracer tracer;
    
    
    public UserController(UserService userService, Tracer tracer) {
        this.userService = userService;
        this.tracer = tracer;
    }
    
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO) {
        Span span = tracer.nextSpan().name("createUser").start();
        try {
            logger.info("POST /api/users - Creating new user: {}", userDTO.getUsername());
            span.tag("user.username", userDTO.getUsername());
            span.tag("user.email", userDTO.getEmail());
            
            UserDTO createdUser = userService.createUser(userDTO);
            span.tag("user.id", createdUser.getId().toString());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            logger.error("Error creating user: {}", e.getMessage());
            span.error(e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating user", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user"));
        } finally {
            span.end();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        Span span = tracer.nextSpan().name("getAllUsers").start();
        try {
            logger.info("GET /api/users - Fetching all users");
            List<UserDTO> users = userService.getAllUsers();
            span.tag("users.count", String.valueOf(users.size()));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Unexpected error fetching users", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long id) {
        Span span = tracer.nextSpan().name("getUserById").start();
        try {
            logger.info("GET /api/users/{} - Fetching user", id);
            span.tag("user.id", id.toString());
            
            Optional<UserDTO> user = userService.getUserById(id);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                span.tag("error", "User not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Unexpected error fetching user", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user"));
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable("username") String username) {
        Span span = tracer.nextSpan().name("getUserByUsername").start();
        try {
            logger.info("GET /api/users/username/{} - Fetching user", username);
            span.tag("user.username", username);
            
            Optional<UserDTO> user = userService.getUserByUsername(username);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                span.tag("error", "User not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Unexpected error fetching user", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user"));
        } finally {
            span.end();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @Valid @RequestBody UserDTO userDTO) {
        Span span = tracer.nextSpan().name("updateUser").start();
        try {
            logger.info("PUT /api/users/{} - Updating user", id);
            span.tag("user.id", id.toString());
            
            UserDTO updatedUser = userService.updateUser(id, userDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating user: {}", e.getMessage());
            span.error(e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating user", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user"));
        } finally {
            span.end();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        Span span = tracer.nextSpan().name("deleteUser").start();
        try {
            logger.info("DELETE /api/users/{} - Deleting user", id);
            span.tag("user.id", id.toString());
            
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting user: {}", e.getMessage());
            span.error(e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Unexpected error deleting user", e);
            span.error(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete user"));
        } finally {
            span.end();
        }
    }
    
    @PostMapping("/{id}/login")
    public ResponseEntity<?> recordLogin(@PathVariable("id") Long id) {
        Span span = tracer.nextSpan().name("recordLogin").start();
        try {
            logger.info("POST /api/users/{}/login - Recording login", id);
            span.tag("user.id", id.toString());
            
            userService.recordUserLogin(id);
            return ResponseEntity.ok(Map.of("message", "Login recorded successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("Error recording login: {}", e.getMessage());
            span.error(e);
            return ResponseEntity.notFound().build();
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        Span span = tracer.nextSpan().name("getUserStats").start();
        try {
            logger.info("GET /api/users/stats - Fetching user statistics");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeUsers", userService.getActiveUserCount());
            stats.put("inactiveUsers", userService.getInactiveUserCount());
            stats.put("totalUsers", userService.getActiveUserCount() + userService.getInactiveUserCount());
            
            span.tag("stats.activeUsers", stats.get("activeUsers").toString());
            span.tag("stats.totalUsers", stats.get("totalUsers").toString());
            
            return ResponseEntity.ok(stats);
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/{id}/exists")
    public ResponseEntity<Map<String, Boolean>> checkUserExists(@PathVariable("id") Long id) {
        Span span = null;
        try {
            span = tracer.nextSpan().name("checkUserExists").start();
            logger.info("GET /api/users/{}/exists - checking user existence", id);
            try { span.tag("user.id", String.valueOf(id)); } catch (Throwable t) { /* ignore */ }
            
            boolean exists = userService.userExists(id);
            logger.info("GET /api/users/{}/exists -> exists={}", id, exists);
            try { span.tag("user.exists", String.valueOf(exists)); } catch (Throwable t) { /* ignore */ }
            
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Throwable e) {
            logger.error("Error checking user existence for ID: {}", id, e);
            if (span != null) { try { span.error(e); } catch (Throwable t) { /* ignore */ } }
            return ResponseEntity.ok(Map.of("exists", false));
        } finally {
            if (span != null) { try { span.end(); } catch (Throwable t) { /* ignore */ } }
        }
    }
}
