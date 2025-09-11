package com.app.shopin.modules.user.controller;

import com.app.shopin.modules.user.dto.NewUserDTO;
import com.app.shopin.modules.user.dto.UpdateEmailDTO;
import com.app.shopin.modules.user.dto.UpdateUserDataDTO;
import com.app.shopin.modules.user.dto.UpdateUsernameDTO;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.service.UserService;
import com.app.shopin.util.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/get")
    public ResponseEntity<List<User>> getAllTheUsers() {
        List<User> users = userService.getAllTheUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping(value = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> save(
            @Valid @ModelAttribute NewUserDTO newUserDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
        // Ya no necesitas construir el baseUrl
        UserResponse respuesta = userService.save(newUserDTO, profileImage); // Se elimina el parámetro
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute NewUserDTO newUserDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
        // Ya no necesitas construir el baseUrl
        UserResponse response = userService.updateUser(id, newUserDTO, profileImage); // Se elimina el parámetro
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable Long id) {
        Map<String, Boolean> response = userService.deleteUser(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update/profile/{id}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDataDTO userDataDTO) {

        UserResponse response = userService.updateUserProfile(id, userDataDTO);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update/username/{id}")
    public ResponseEntity<UserResponse> updateUsername(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUsernameDTO usernameDTO) {

        UserResponse response = userService.updateUsername(id, usernameDTO);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update/email/{id}")
    public ResponseEntity<UserResponse> updateEmail(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailDTO emailDTO) {

        UserResponse response = userService.updateEmail(id, emailDTO);
        return ResponseEntity.ok(response);
    }

}
