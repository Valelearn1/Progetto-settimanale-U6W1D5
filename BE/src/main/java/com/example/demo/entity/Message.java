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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    /**
     * IDENTITY e non UUID: questa e' una tabella in sola aggiunta, mai esposta
     * per id nell'interfaccia. L'id sequenziale da' un indice piu' compatto e
     * un ordine di inserimento naturale.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * LAZY: in serializzazione non serve mai l'oggetto Chat, basta il suo id
     * che e' gia' nella colonna. Caricarla a ogni messaggio sarebbe una join
     * pagata per niente.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    /**
     * EAGER: il DTO legge username, displayName e avatarColor del mittente
     * durante la serializzazione, cioe' FUORI dalla transazione, perche' il
     * progetto gira con spring.jpa.open-in-view=false. Con un proxy lazy
     * scatterebbe una LazyInitializationException.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    /** Null finche' il destinatario non apre la conversazione. */
    @Column(name = "read_at")
    private Instant readAt;

    public Message(Chat chat, User sender, String content) {
        this.chat = chat;
        this.sender = sender;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }
}
