package com.example.demo.config;

import com.example.demo.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsSource;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            // Anche mvcHandlerMappingIntrospector implementa CorsConfigurationSource:
            // senza @Qualifier Spring non saprebbe quale dei due bean usare.
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsSource = corsSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsSource))
                // Niente CSRF: non usiamo cookie di sessione, il token viaggia
                // nell'header Authorization e va aggiunto esplicitamente dal
                // JavaScript, quindi l'attacco che il CSRF previene non si applica.
                .csrf(csrf -> csrf.disable())
                // Niente sessioni lato server: ogni richiesta porta il suo token.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Registrazione, verifica e login devono essere raggiungibili
                        // da chi non ha ancora un token.
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify",
                                "/api/auth/login").permitAll()
                        // L'handshake del WebSocket arriva SENZA header Authorization:
                        // l'API del browser non permette di aggiungerlo. Se lo
                        // bloccassimo qui con un 401, il frame CONNECT - che il token
                        // invece ce l'ha - non verrebbe mai spedito.
                        // L'autenticazione vera avviene in StompAuthChannelInterceptor.
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Di default Spring Security risponderebbe con una pagina di login HTML.
     * Qui siamo un'API: serve un 401 con un JSON che il frontend sa leggere.
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"stato\":401,\"messaggio\":\"Devi effettuare l'accesso.\"}");
        };
    }

    /**
     * BCrypt: le password non vengono mai salvate in chiaro e nemmeno cifrate,
     * ma trasformate in un hash non reversibile con un "sale" casuale.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
