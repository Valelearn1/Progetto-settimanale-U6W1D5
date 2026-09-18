package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyRequestDTO(

        @NotBlank(message = "l'email è obbligatoria")
        @Email(message = "l'email non è in un formato valido")
        String email,

        @NotBlank(message = "il codice è obbligatorio")
        String codice) {
}
