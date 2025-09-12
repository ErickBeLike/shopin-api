package com.app.shopin.modules.security.dto;


import jakarta.validation.constraints.NotBlank;

public class LoginDTO {
    @NotBlank(message = "nombre de usuario/email obligatorio")
    private String usernameOrEmail;

    @NotBlank(message = "contraseña obligatoria")
    private String password;

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

