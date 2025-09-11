package com.app.shopin.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserDataDTO(
        @NotBlank(message = "El nombre no puede estar vacío")
        @Size(max = 100, message = "El nombre no debe exceder los 100 caracteres")
        String firstName,

        @NotBlank(message = "El apellido no puede estar vacío")
        @Size(max = 100, message = "El apellido no debe exceder los 100 caracteres")
        String lastName,

        @Size(max = 20, message = "El teléfono no debe exceder los 20 caracteres")
        String phone
) {}
