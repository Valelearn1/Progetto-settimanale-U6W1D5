package com.example.demo.controller;

import com.example.demo.dto.response.PeerStatsDTO;
import com.example.demo.dto.response.StatsResponseDTO;
import com.example.demo.entity.User;
import com.example.demo.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public StatsResponseDTO mie(@AuthenticationPrincipal User io) {
        return statsService.calcola(io);
    }

    /** I nodi del grafo dei contatti (§9.2 della progettazione). */
    @GetMapping("/network")
    public List<PeerStatsDTO> rete(@AuthenticationPrincipal User io) {
        return statsService.rete(io);
    }

    /**
     * Risponde 202 e non 200: quando il backend risponde, l'email e' in coda
     * per l'invio dopo il commit, non ancora consegnata. Dire 200 ("fatto")
     * sarebbe una piccola bugia su cui il frontend costruirebbe il messaggio
     * sbagliato.
     */
    @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> perEmail(@AuthenticationPrincipal User io) {
        StatsResponseDTO stats = statsService.inviaPerEmail(io);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "messaggio", "Report in arrivo su " + io.getEmail(),
                "statistiche", stats));
    }
}
