package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Il client HTTP verso OpenRouter: qui vive la parte "fissa" del comando cURL
 * (URL di base, Content-Type, timeout). L'header Authorization viene aggiunto a
 * ogni richiesta da OpenRouterService, perche' contiene il segreto e non va
 * lasciato in un bean di configurazione.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient openRouterClient(OpenRouterProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Generoso di proposito: con il ragionamento acceso la risposta puo'
        // farsi attendere parecchio.
        requestFactory.setReadTimeout(Duration.ofSeconds(120));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("HTTP-Referer", properties.getReferer())
                .defaultHeader("X-Title", properties.getAppTitle())
                .requestFactory(requestFactory)
                .build();
    }
}
