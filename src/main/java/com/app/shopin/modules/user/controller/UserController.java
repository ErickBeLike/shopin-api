package com.app.shopin.modules.user.controller;

import com.app.shopin.modules.security.dto.*;
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
    public ResponseEntity<Map<String, Boolean>> softDeleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails currentUser) { // <--- Inyecta el usuario autenticado

        Map<String, Boolean> response = userService.softDeleteUser(userId, currentUser); // <--- Pasa el usuario autenticado al servicio
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

    // 2FA APP SECTION
    @PostMapping("/{userId}/2fa/app/setup")
    public ResponseEntity<SetupTwoFactorDTO> setupApp2FA(
            @PathVariable Long userId,
            @RequestBody @Valid PasswordConfirmationDTO dto) { // <- AHORA PIDE CONTRASEÑA
        SetupTwoFactorDTO response = userService.setupAppTwoFactor(userId, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/2fa/app/enable")
    public ResponseEntity<UserResponse> enableApp2FA(
            @PathVariable Long userId,
            @RequestBody @Valid CodeConfirmationDTO  dto) {
        UserResponse response = userService.enableAppTwoFactor(userId, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/2fa/app/pre-disable")
    public ResponseEntity<UserResponse> preDisableApp2FA(
            @PathVariable Long userId,
            @RequestBody @Valid PasswordConfirmationDTO dto) {
        return ResponseEntity.ok(userService.preDisableAppTwoFactor(userId, dto));
    }

    @PostMapping("/{userId}/2fa/app/confirm-disable")
    public ResponseEntity<UserResponse> confirmDisableApp2FA(
            @PathVariable Long userId,
            @RequestBody @Valid CodeConfirmationDTO dto) {
        return ResponseEntity.ok(userService.confirmDisableAppTwoFactor(userId, dto));
    }

    // 2FA EMAIL SECTION
    @PostMapping("/{userId}/2fa/email/setup")
    public ResponseEntity<UserResponse> preEnableEmail2FA(
            @PathVariable Long userId,
            @RequestBody @Valid PasswordConfirmationDTO dto) {
        return ResponseEntity.ok(userService.setupEmailTwoFactor(userId, dto));
    }

    @PostMapping("/{userId}/2fa/email/enable")
    public ResponseEntity<UserResponse> confirmEnableEmail2FA(
            @PathVariable Long userId,
            @RequestBody @Valid CodeConfirmationDTO  dto) {
        return ResponseEntity.ok(userService.enableEmailTwoFactor(userId, dto));
    }

    @PostMapping("/{userId}/2fa/email/pre-disable")
    public ResponseEntity<UserResponse> preDisableEmail2FA(
            @PathVariable Long userId,
            @RequestBody @Valid PasswordConfirmationDTO dto) {
        return ResponseEntity.ok(userService.preDisableEmailTwoFactor(userId, dto));
    }

    @PostMapping("/{userId}/2fa/email/confirm-disable")
    public ResponseEntity<UserResponse> confirmDisableEmail2FA(
            @PathVariable Long userId,
            @RequestBody @Valid CodeConfirmationDTO  dto) {
        return ResponseEntity.ok(userService.confirmDisableEmailTwoFactor(userId, dto));
    }

    // 2FA PREFERRED METHOD SECTION
    @PostMapping("/{userId}/2fa/set-preferred")
    public ResponseEntity<UserResponse> setPreferred2FA(
            @PathVariable Long userId,
            @RequestBody @Valid SetPreferredTwoFactorDTO dto) {
        return ResponseEntity.ok(userService.setPreferredTwoFactorMethod(userId, dto.method()));
    }

    // UN-LINK ACCOUNT
    @DeleteMapping("/{userId}/social-links/{provider}")
    public ResponseEntity<UserResponse> unlinkSocialAccount(
            @PathVariable Long userId,
            @PathVariable String provider,
            @RequestBody @Valid PasswordConfirmationDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {

        UserResponse response = userService.unlinkSocialAccount(userId, provider, dto, currentUser);
        return ResponseEntity.ok(response);
    }
}
