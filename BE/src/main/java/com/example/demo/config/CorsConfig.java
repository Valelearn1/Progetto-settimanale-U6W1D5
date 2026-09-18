package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Senza questa configurazione il browser blocca ogni chiamata del frontend:
 * Vite gira su :5173 e il backend su :8080, quindi sono due origini diverse.
 *
 * <p>E' esposta come {@link CorsConfigurationSource} e non come
 * {@code WebMvcConfigurer} perche' le richieste passano prima dalla catena di
 * filtri di Spring Security: il CORS va applicato li', altrimenti il preflight
 * OPTIONS verrebbe respinto con 401 prima ancora di arrivare al controller.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // In sviluppo Vite sceglie una porta diversa (5174, 5175...) se la 5173
        // e' occupata: fissare la sola 5173 farebbe fallire il frontend in modo
        // difficile da diagnosticare.
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
