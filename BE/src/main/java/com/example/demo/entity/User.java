package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Un utente registrato.
 *
 * <p>Ha due identita' distinte di proposito: la <em>email</em> serve ad accedere
 * e a ricevere le notifiche, lo <em>username</em> e' il nome pubblico mostrato
 * in chat. Tenerle separate evita di esporre l'email di tutti dentro la rubrica
 * dei contatti.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Hash BCrypt: la password in chiaro non viene mai salvata. */
    @Column(nullable = false)
    private String password;

    /** Colore dell'avatar, assegnato alla creazione. Salvato come esadecimale. */
    @Column(name = "avatar_color", nullable = false, length = 7)
    private String avatarColor;

    /**
     * Falso finche' l'utente non conferma l'email. Un account non attivo non
     * puo' accedere e non compare nella rubrica degli altri.
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Le conversazioni che ho messo fra i preferiti.
     *
     * <p>Sta su User e non su Chat perche' "preferita" e' una proprieta' MIA:
     * un flag sulla conversazione la marcherebbe anche per l'altra persona.
     *
     * <p>LAZY di proposito: l'utente viene caricato a ogni richiesta dal filtro
     * JWT, e tirarsi dietro l'elenco delle preferite ogni volta sarebbe una
     * join pagata per niente. Chi ne ha bisogno usa le query dedicate in
     * ChatRepository, che restituiscono solo gli id.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_favorites",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "chat_id"))
    private Set<Chat> preferite = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
