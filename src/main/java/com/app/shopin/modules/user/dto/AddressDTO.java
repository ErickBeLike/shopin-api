package com.app.shopin.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressDTO(
        Long id,
        @NotBlank(message = "La calle no puede estar vacía")
        @Size(max = 200)
        String street,

        @Size(max = 100)
        String internalDetails,

        @NotBlank(message = "La ciudad no puede estar vacía")
        @Size(max = 100)
        String city,

        @NotBlank(message = "El estado no puede estar vacío")
        @Size(max = 100)
        String state,

        @NotBlank(message = "El código postal no puede estar vacío")
        @Size(max = 10)
        String postalCode,

        @NotBlank(message = "El país no puede estar vacío")
        @Size(max = 100)
        String country
) {}