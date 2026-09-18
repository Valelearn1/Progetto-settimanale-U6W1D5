package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "lo username è obbligatorio")
        @Size(min = 3, max = 30, message = "lo username deve avere fra 3 e 30 caratteri")
        // Niente spazi ne' simboli: lo username finisce nelle destinazioni STOMP
        // /user/{username}/queue/..., dove certi caratteri creerebbero problemi.
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "lo username può contenere solo lettere, numeri, punto, trattino e underscore")
        String username,

        @NotBlank(message = "il nome visualizzato è obbligatorio")
        @Size(max = 60, message = "il nome visualizzato non può superare i 60 caratteri")
        String displayName,

        @NotBlank(message = "l'email è obbligatoria")
        @Email(message = "l'email non è in un formato valido")
        @Size(max = 120, message = "l'email non può superare i 120 caratteri")
        String email,

        @NotBlank(message = "la password è obbligatoria")
        @Size(min = 8, max = 72, message = "la password deve avere fra 8 e 72 caratteri")
        // 72 e' il limite di BCrypt: oltre quella lunghezza i caratteri in piu'
        // vengono ignorati, quindi e' meglio rifiutarli che dare falsa sicurezza.
        String password) {
}
