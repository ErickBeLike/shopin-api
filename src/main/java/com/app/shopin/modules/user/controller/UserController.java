package com.app.shopin.modules.user.controller;

import com.app.shopin.modules.user.dto.*;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.service.UserService;
import com.app.shopin.util.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllTheUsers() {
        List<User> users = userService.getAllTheUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> save(
            @Valid @ModelAttribute NewUserDTO newUserDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
        // Ya no necesitas construir el baseUrl
        UserResponse respuesta = userService.save(newUserDTO, profileImage); // Se elimina el parámetro
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PutMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @ModelAttribute NewUserDTO newUserDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
        // Ya no necesitas construir el baseUrl
        UserResponse response = userService.updateUser(userId, newUserDTO, profileImage); // Se elimina el parámetro
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable Long userId) {
        Map<String, Boolean> response = userService.deleteUser(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserDataDTO userDataDTO) {

        UserResponse response = userService.updateUserProfile(userId, userDataDTO);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateUserImage(
            @PathVariable Long userId,
            @RequestParam("profileImage") MultipartFile profileImage) {

        UserResponse response = userService.updateProfileImage(userId, profileImage);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/profile-image")
    public ResponseEntity<UserResponse> deleteUserImage(@PathVariable Long userId) {
        UserResponse response = userService.deleteProfileImage(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/username")
    public ResponseEntity<UserResponse> updateUsername(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUsernameDTO usernameDTO) {

        UserResponse response = userService.updateUsername(userId, usernameDTO);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/email")
    public ResponseEntity<UserResponse> updateEmail(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateEmailDTO emailDTO) {

        UserResponse response = userService.updateEmail(userId, emailDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/change-password")
    public ResponseEntity<UserResponse> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO,
            @AuthenticationPrincipal UserDetails currentUser) {

        // El controlador ahora solo pasa los datos al servicio. Limpio y simple.
        UserResponse response = userService.changePassword(
                userId,
                changePasswordDTO.oldPassword(),
                changePasswordDTO.newPassword(),
                currentUser
        );
        return ResponseEntity.ok(response);
    }

}
