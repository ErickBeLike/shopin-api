package com.app.shopin.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class NewUserDTO {
    private MultipartFile profileImage;
    @NotBlank(message = "nombre de usuario obligatorio")
    private String username;
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser una dirección de correo válida")
    private String email;
    @NotBlank
    private String password;
    private Set<String> roles;

    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    private String phone;

    public NewUserDTO() {
    }

    public NewUserDTO(MultipartFile profileImage,
                      String username,
                      String email,
                      String password,
                      Set<String> roles,
                      String firstName,
                      String lastName,
                      String phone) {
        this.profileImage = profileImage;
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public @NotBlank(message = "nombre de usuario obligatorio") String getUsername() {
        return username;
    }

    public void setUserName(@NotBlank(message = "nombre de usuario obligatorio") String username) {
        this.username = username;
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

    public @NotBlank(message = "El nombre es obligatorio") String getFirstName() {
        return firstName;
    }

    public void setFirstName(@NotBlank(message = "El nombre es obligatorio") String firstName) {
        this.firstName = firstName;
    }

    public @NotBlank(message = "El apellido es obligatorio") String getLastName() {
        return lastName;
    }

    public void setLastName(@NotBlank(message = "El apellido es obligatorio") String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public MultipartFile getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(MultipartFile profileImage) {
        this.profileImage = profileImage;
    }

}
