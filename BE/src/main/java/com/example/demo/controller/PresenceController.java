package com.example.demo.controller;

import com.example.demo.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Chi e' online adesso.
 *
 * <p>Serve accanto al broadcast su /topic/presence, non al suo posto: il
 * broadcast dice solo cosa cambia <em>da adesso in poi</em>. Quando ti colleghi
 * devi sapere chi era <b>gia'</b> online prima che tu arrivassi, e quella e'
 * una domanda con una risposta sola — quindi va su HTTP.
 */
@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping
    public Set<String> online() {
        return presenceService.online();
    }
}
