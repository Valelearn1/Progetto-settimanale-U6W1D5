package com.example.demo.dto.response;

import com.example.demo.entity.User;

import java.util.UUID;

/**
 * L'utente come lo vedono gli altri. Non contiene email ne' password: la
 * rubrica e' condivisa fra tutti gli iscritti, e l'email e' un dato di contatto
 * privato che non c'e' motivo di far circolare.
 */
public record UserDTO(UUID id, String username, String displayName, String avatarColor) {

    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarColor());
    }
}
