package com.example.demo.controller;

import com.example.demo.dto.request.SuggestionRequestDTO;
import com.example.demo.dto.response.SuggestionResponseDTO;
import com.example.demo.entity.User;
import com.example.demo.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST e non GET, pur essendo una lettura: ogni chiamata consuma quota su un
 * servizio esterno e produce un risultato diverso ogni volta. Un GET
 * inviterebbe browser e proxy a metterlo in cache, che e' l'opposto di cio'
 * che serve.
 */
@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping
    public SuggestionResponseDTO suggerisci(
            @AuthenticationPrincipal User io,
            @Valid @RequestBody SuggestionRequestDTO dto) {
        return suggestionService.suggerisci(io, dto.chatId());
    }
}
