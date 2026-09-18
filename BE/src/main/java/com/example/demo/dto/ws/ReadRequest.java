package com.example.demo.dto.ws;

import java.util.UUID;

/** "Ho aperto questa conversazione": segna letti i messaggi dell'altro. */
public record ReadRequest(UUID chatId) {
}
