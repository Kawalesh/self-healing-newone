package com.observability.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class UserDTO {
    
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean active;
    private int loginCount;
    
    // Constructors
    public UserDTO() {}
    
    public UserDTO(Long id, String username, String email, String fullName, 
                   LocalDateTime createdAt, LocalDateTime lastLoginAt, 
                   boolean active, int loginCount) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.active = active;
        this.loginCount = loginCount;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public int getLoginCount() {
        return loginCount;
    }
    
    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }
}

