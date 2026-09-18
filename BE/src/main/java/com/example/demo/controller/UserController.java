package com.example.demo.controller;

import com.example.demo.dto.response.UserDTO;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * L'utente autenticato arriva da {@code @AuthenticationPrincipal}, cioe' dal
 * token verificato in JwtAuthFilter. Non viene mai letto da un parametro o da
 * un header scelto dal client: e' cosi' che si impedisce di agire a nome di
 * qualcun altro.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDTO> rubrica(@AuthenticationPrincipal User utente) {
        return userService.rubricaPer(utente);
    }

    @GetMapping("/me")
    public UserDTO io(@AuthenticationPrincipal User utente) {
        return UserDTO.from(utente);
    }
}
