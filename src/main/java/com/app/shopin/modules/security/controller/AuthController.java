package com.app.shopin.modules.security.controller;


import com.app.shopin.modules.security.dto.JwtDTO;
import com.app.shopin.modules.security.dto.LoginDTO;
import com.app.shopin.modules.security.dto.NewUserDTO;
import com.app.shopin.modules.security.entity.User;
import com.app.shopin.modules.security.service.UserService;
import com.app.shopin.util.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    UserService userService;

    @PostMapping(value = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> save(
            @Valid @ModelAttribute NewUserDTO newUserDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            HttpServletRequest request) {
        // Construye el baseUrl dinámicamente:
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();
        UserResponse respuesta = userService.save(newUserDTO, profileImage, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }


    @PostMapping("/login")
    public ResponseEntity<JwtDTO> login(@Valid @RequestBody LoginDTO loginDTO){
        return ResponseEntity.ok(userService.login(loginDTO));
    }

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

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute NewUserDTO newUserDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            HttpServletRequest request) {

        // Construye el baseUrl (p.ej. http://localhost:8080)
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();

        UserResponse response = userService.updateUser(id, newUserDTO, profileImage, baseUrl);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable Long id) {
        Map<String, Boolean> response = userService.deleteUser(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDTO> refresh(@RequestBody JwtDTO jwtDTO) throws ParseException {
        return ResponseEntity.ok(userService.refresh(jwtDTO));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            userService.logout(token);
            return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Token no proporcionado"));
    }

}
