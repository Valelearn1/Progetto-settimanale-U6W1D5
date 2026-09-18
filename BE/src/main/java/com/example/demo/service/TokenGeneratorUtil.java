package com.example.demo.service;

import java.security.SecureRandom;

/**
 * Genera i codici casuali dell'applicazione.
 *
 * <p>Usa {@link SecureRandom} e non {@code Math.random()}: un generatore
 * normale produce sequenze prevedibili a partire dal seme, e un codice di
 * attivazione indovinabile vanifica la conferma via email.
 */
public final class TokenGeneratorUtil {

    private static final String ALFANUMERICI =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGeneratorUtil() {
    }

    public static String randomAlphanumeric(int lunghezza) {
        StringBuilder sb = new StringBuilder(lunghezza);
        for (int i = 0; i < lunghezza; i++) {
            sb.append(ALFANUMERICI.charAt(RANDOM.nextInt(ALFANUMERICI.length())));
        }
        return sb.toString();
    }
}
