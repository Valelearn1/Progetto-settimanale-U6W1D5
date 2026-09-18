package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Popola la rubrica con qualche persona, così l'applicazione non si apre vuota.
 *
 * <p>Sono <b>utenti veri</b> salvati sul database, non un elenco finto nel
 * frontend: un array di nomi lato client non sarebbe cliccabile, perché il
 * backend rifiuterebbe qualunque conversazione verso un id inesistente.
 *
 * <p>Si disattiva con {@code app.seed.enabled=false}. Ogni utente viene creato
 * solo se la sua email non c'è già, quindi riavviare l'applicazione non
 * duplica niente e non sovrascrive eventuali modifiche.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    /** Password unica per tutti i profili dimostrativi. */
    private static final String PASSWORD = "password123";

    private record Profilo(String username, String displayName, String email, String colore) {}

    private static final List<Profilo> PROFILI = List.of(
            new Profilo("giulia", "Giulia Ferrari", "giulia@filorosso.demo", "#e0485b"),
            new Profilo("luca", "Luca Bianchi", "luca@filorosso.demo", "#4aa8ff"),
            new Profilo("sofia", "Sofia Romano", "sofia@filorosso.demo", "#f0b429"),
            new Profilo("matteo", "Matteo Greco", "matteo@filorosso.demo", "#2dd4bf"),
            new Profilo("chiara", "Chiara Costa", "chiara@filorosso.demo", "#a78bfa"),
            new Profilo("davide", "Davide Marino", "davide@filorosso.demo", "#84cc16"));

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        long creati = PROFILI.stream().filter(this::creaSeManca).count();

        if (creati > 0) {
            log.info("Rubrica dimostrativa: {} profili creati (password: {})", creati, PASSWORD);
        }
    }

    private boolean creaSeManca(Profilo profilo) {
        if (userRepository.existsByEmail(profilo.email())) {
            return false;
        }

        User utente = new User();
        utente.setUsername(profilo.username());
        utente.setDisplayName(profilo.displayName());
        utente.setEmail(profilo.email());
        utente.setPassword(passwordEncoder.encode(PASSWORD));
        utente.setAvatarColor(profilo.colore());
        // Già attivi: sono profili di prova, non devono passare dalla conferma email.
        utente.setActive(true);
        userRepository.save(utente);
        return true;
    }
}
