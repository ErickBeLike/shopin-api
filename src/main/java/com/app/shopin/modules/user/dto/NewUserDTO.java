package com.app.shopin.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Set;

public class NewUserDTO {
    private MultipartFile profileImage;
    @NotBlank(message = "nombre de usuario obligatorio")
    private String userName;
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    private Set<String> roles;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NewUserDTO() {
    }

    public NewUserDTO(MultipartFile profileImage, String userName, String email, String password, Set<String> roles) {
        this.profileImage = profileImage;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public @NotBlank(message = "nombre de usuario obligatorio") String getUserName() {
        return userName;
    }

    public void setUserName(@NotBlank(message = "nombre de usuario obligatorio") String userName) {
        this.userName = userName;
    }

    public @NotBlank String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank String email) {
        this.email = email;
    }

    public @NotBlank String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public MultipartFile getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(MultipartFile profileImage) {
        this.profileImage = profileImage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
