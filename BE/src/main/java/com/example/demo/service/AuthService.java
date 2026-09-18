package com.example.demo.service;

import com.example.demo.dto.request.LoginRequestDTO;
import com.example.demo.dto.request.RegisterRequestDTO;
import com.example.demo.dto.request.VerifyRequestDTO;
import com.example.demo.dto.response.AuthResponseDTO;
import com.example.demo.dto.response.UserDTO;
import com.example.demo.entity.RegistrationToken;
import com.example.demo.entity.User;
import com.example.demo.event.MailEvents;
import com.example.demo.exception.EmailAlreadyExistsException;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.exception.InvalidTokenException;
import com.example.demo.exception.TokenExpiredException;
import com.example.demo.exception.UserNotActiveException;
import com.example.demo.repository.RegistrationTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int LUNGHEZZA_CODICE = 32;

    /** Colori assegnati a rotazione agli avatar, per distinguere i contatti a colpo d'occhio. */
    private static final List<String> PALETTE = List.of(
            "#3ecf8e", "#4aa8ff", "#f0b429", "#e8618c",
            "#a78bfa", "#2dd4bf", "#fb7185", "#84cc16");

    private final UserRepository userRepository;
    private final RegistrationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void register(RegisterRequestDTO dto) {
        String email = dto.email().trim().toLowerCase();
        String username = dto.username().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Questa email è già registrata");
        }
        if (userRepository.existsByUsername(username)) {
            throw new EmailAlreadyExistsException("Questo username è già in uso");
        }

        User user = new User();
        user.setUsername(username);
        user.setDisplayName(dto.displayName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setAvatarColor(PALETTE.get((int) (userRepository.count() % PALETTE.size())));
        // Resta inattivo finche' non conferma l'email.
        user.setActive(false);
        user = userRepository.save(user);

        String codice = TokenGeneratorUtil.randomAlphanumeric(LUNGHEZZA_CODICE);
        tokenRepository.save(new RegistrationToken(codice, user));

        // L'email parte solo dopo il commit: vedi MailEventListener.
        // Il codice NON finisce nel log: chi legge i log potrebbe attivare
        // account altrui. Si registra il fatto, non il segreto.
        log.info("Registrazione: nuovo utente '{}' ({}), in attesa di conferma",
                user.getUsername(), user.getEmail());

        events.publishEvent(new MailEvents.RegistrationRequested(
                user.getEmail(), user.getDisplayName(), codice));
    }

    @Transactional
    public void verify(VerifyRequestDTO dto) {
        String email = dto.email().trim().toLowerCase();

        RegistrationToken token = tokenRepository
                .findByValueAndUser_Email(dto.codice().trim(), email)
                .orElseThrow(() -> new InvalidTokenException("Codice non valido"));

        if (token.isUsed()) {
            throw new InvalidTokenException("Questo codice è già stato usato");
        }
        if (token.isScaduto()) {
            throw new TokenExpiredException(
                    "Codice scaduto: richiedi una nuova registrazione");
        }

        User user = token.getUser();
        user.setActive(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        // La rubrica e' condivisa: avvisa i client collegati che c'e' un contatto
        // nuovo, cosi' le liste si aggiornano senza dover ricaricare la pagina.
        log.info("Attivazione: '{}' ha confermato l'email", user.getUsername());
        messagingTemplate.convertAndSend("/topic/users", UserDTO.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO dto) {
        String email = dto.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login fallito: nessun account per {}", email);
                    return new InvalidCredentialsException("Email o password non validi");
                });

        // Stesso messaggio per utente inesistente e password errata: distinguerli
        // direbbe a un attaccante quali email risultano registrate.
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            // Si registra il tentativo, MAI la password provata.
            log.warn("Login fallito: password errata per {}", email);
            throw new InvalidCredentialsException("Email o password non validi");
        }

        if (!user.isActive()) {
            log.warn("Login rifiutato: account '{}' non ancora attivato", user.getUsername());
            throw new UserNotActiveException(
                    "Conferma prima la tua email: ti abbiamo inviato un codice di attivazione");
        }

        log.info("Login riuscito: '{}'", user.getUsername());
        return new AuthResponseDTO(jwtService.generateToken(user), UserDTO.from(user));
    }
}
