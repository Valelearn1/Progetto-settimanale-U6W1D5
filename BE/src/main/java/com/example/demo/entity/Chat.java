package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Una conversazione fra due utenti.
 *
 * <p>I partecipanti sono una {@code @ManyToMany} verso {@link User}: la tabella
 * di collegamento la crea JPA e non serve un'entita' propria, perche' non deve
 * portare nessun dato suo.
 */
@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * I due id ordinati e uniti da ":" — vedi {@link #pairKey(UUID, UUID)}.
     *
     * <p>Il vincolo di unicita' e' l'unica difesa contro una race condition
     * reale: se due persone si scrivono per la prima volta nello stesso
     * istante, senza questo indice nascerebbero DUE conversazioni fra le stesse
     * persone e i messaggi si dividerebbero fra le due senza che nessuno se ne
     * accorga. Cosi' invece e' il database a rifiutare il duplicato.
     */
    @Column(name = "pair_key", nullable = false, unique = true, length = 73)
    private String pairKey;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "chat_participants",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> participants = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Quando e' arrivato l'ultimo messaggio. Nullo finche' la chat e' vuota.
     *
     * <p>E' l'unica ridondanza accettata nel modello: serve a ordinare la
     * sidebar per attivita' recente senza dover cercare, per ogni chat, il suo
     * messaggio piu' recente. Si aggiorna in un solo punto (ChatService.invia),
     * quindi non puo' andare fuori sincrono come farebbe un contatore.
     */
    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public Chat(User a, User b) {
        this.pairKey = pairKey(a.getId(), b.getId());
        this.participants = new HashSet<>(Set.of(a, b));
    }

    /**
     * Costruisce la chiave di coppia. L'ordinamento e' cio' che la rende
     * identica sia che scriva A a B, sia che scriva B ad A: senza, la stessa
     * conversazione avrebbe due chiavi diverse e il vincolo non servirebbe a
     * niente.
     */
    public static String pairKey(UUID a, UUID b) {
        String sa = a.toString();
        String sb = b.toString();
        return sa.compareTo(sb) < 0 ? sa + ":" + sb : sb + ":" + sa;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** L'altro partecipante, visto da chi sta guardando la conversazione. */
    public User altroRispettoA(User io) {
        return participants.stream()
                .filter(u -> !u.getId().equals(io.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Chat " + id + " senza un secondo partecipante"));
    }

    public boolean partecipa(User utente) {
        return participants.stream().anyMatch(u -> u.getId().equals(utente.getId()));
    }
}
