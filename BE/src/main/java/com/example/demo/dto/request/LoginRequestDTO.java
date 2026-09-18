package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "l'email è obbligatoria")
        @Email(message = "l'email non è in un formato valido")
        String email,

        @NotBlank(message = "la password è obbligatoria")
        String password) {
}
