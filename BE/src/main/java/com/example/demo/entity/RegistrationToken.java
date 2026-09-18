package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Il codice inviato per email alla registrazione.
 *
 * <p>E' una tabella a parte e non due colonne su {@link User}: cosi' un utente
 * puo' chiedere un codice nuovo se il primo scade, e lo storico dei tentativi
 * resta leggibile. Il flag {@code used} impedisce che lo stesso link, magari
 * rimasto nella cronologia del browser, riattivi un account disattivato in
 * seguito.
 */
@Entity
@Table(name = "registration_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationToken {

    /** Quanto resta valido il codice dal momento in cui viene creato. */
    public static final int VALIDITA_ORE = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expire_at", nullable = false)
    private Instant expireAt;

    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    public RegistrationToken(String value, User user) {
        this.value = value;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expireAt == null) {
            expireAt = createdAt.plus(VALIDITA_ORE, ChronoUnit.HOURS);
        }
    }

    public boolean isScaduto() {
        return expireAt.isBefore(Instant.now());
    }
}
