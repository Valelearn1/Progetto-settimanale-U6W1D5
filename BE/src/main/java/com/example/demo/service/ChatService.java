package com.example.demo.service;

import com.example.demo.dto.response.ChatSummaryDTO;
import com.example.demo.dto.response.MessageDTO;
import com.example.demo.dto.response.UserDTO;
import com.example.demo.dto.ws.IncomingMessage;
import com.example.demo.entity.Chat;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int LUNGHEZZA_ANTEPRIMA = 80;

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Trova la conversazione con questa persona, o la crea se non esiste.
     *
     * <p>Il {@code catch} sulla violazione di unicita' non e' difensivismo: se
     * due persone si scrivono per la prima volta nello stesso istante, entrambe
     * le richieste trovano "nessuna chat" e provano a inserirla. Il vincolo su
     * pairKey fa fallire la seconda, e a quel punto la chat esiste: basta
     * rileggerla invece di propagare l'errore all'utente.
     */
    @Transactional
    public Chat trovaOCrea(User io, UUID peerId) {
        if (io.getId().equals(peerId)) {
            throw new IllegalArgumentException("Non puoi aprire una conversazione con te stesso");
        }

        User peer = userService.findById(peerId);
        if (!peer.isActive()) {
            throw new IllegalArgumentException("Questo utente non ha ancora confermato l'email");
        }

        String chiave = Chat.pairKey(io.getId(), peer.getId());
        return chatRepository.findByPairKey(chiave)
                .orElseGet(() -> {
                    try {
                        return chatRepository.saveAndFlush(new Chat(io, peer));
                    } catch (DataIntegrityViolationException gara) {
                        return chatRepository.findByPairKey(chiave)
                                .orElseThrow(() -> gara);
                    }
                });
    }

    /**
     * Salva il messaggio e restituisce il DTO da consegnare.
     *
     * <p>Il mittente arriva dal Principal della sessione, non dal payload, e la
     * partecipazione alla chat viene verificata qui: sono i due controlli che
     * impediscono di scrivere a nome di altri o dentro conversazioni altrui.
     */
    @Transactional
    public MessageDTO invia(User mittente, IncomingMessage incoming) {
        if (incoming.content() == null || incoming.content().isBlank()) {
            throw new IllegalArgumentException("Il messaggio è vuoto");
        }

        Chat chat = caricaPartecipata(incoming.chatId(), mittente);

        Message salvato = messageRepository.save(
                new Message(chat, mittente, incoming.content().trim()));

        // Unico punto in cui lastMessageAt viene aggiornato: e' per questo che
        // non puo' andare fuori sincrono.
        chat.setLastMessageAt(salvato.getSentAt());
        chatRepository.save(chat);

        return MessageDTO.from(salvato);
    }

    /**
     * Segna letti i messaggi che l'altro mi ha mandato in questa chat e
     * restituisce i DTO aggiornati. Lista vuota se non c'era niente da leggere:
     * il chiamante evita cosi' di spedire notifiche inutili.
     */
    @Transactional
    public List<MessageDTO> segnaLetti(User io, UUID chatId) {
        caricaPartecipata(chatId, io);

        List<Message> daLeggere = messageRepository.findNonLetti(chatId, io.getId());
        if (daLeggere.isEmpty()) {
            return List.of();
        }

        Instant adesso = Instant.now();
        daLeggere.forEach(m -> m.setReadAt(adesso));

        return messageRepository.saveAll(daLeggere).stream().map(MessageDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> cronologia(User io, UUID chatId) {
        caricaPartecipata(chatId, io);
        return messageRepository.findByChat_IdOrderBySentAtAsc(chatId).stream()
                .map(MessageDTO::from)
                .toList();
    }

    /**
     * La sidebar: preferite in cima, poi le altre per attivita' recente.
     *
     * <p>Gli id delle preferite arrivano da UNA query, non da una per riga:
     * con venti conversazioni la differenza fra 1 e 21 interrogazioni al
     * database si sente gia'.
     */
    @Transactional(readOnly = true)
    public List<ChatSummaryDTO> sidebar(User io) {
        Set<UUID> preferite = chatRepository.idPreferite(io.getId());

        return chatRepository.findMie(io.getId()).stream()
                .map(chat -> riassumi(chat, io, preferite.contains(chat.getId())))
                .sorted(Comparator
                        .comparing(ChatSummaryDTO::preferita).reversed()
                        .thenComparing(ChatSummaryDTO::lastMessageAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Mette o toglie una conversazione dai preferiti.
     *
     * <p>L'utente viene riletto dal repository e non preso dal SecurityContext:
     * quello e' un'entita' staccata dalla sessione JPA, e toccarne la
     * collezione lazy fuori dalla transazione non avrebbe alcun effetto.
     */
    @Transactional
    public ChatSummaryDTO cambiaPreferita(User io, UUID chatId) {
        Chat chat = caricaPartecipata(chatId, io);
        User gestito = userRepository.findById(io.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        boolean eraPreferita = gestito.getPreferite().removeIf(c -> c.getId().equals(chatId));
        if (!eraPreferita) {
            gestito.getPreferite().add(chat);
        }
        userRepository.save(gestito);

        return riassumi(chat, io, !eraPreferita);
    }

    @Transactional(readOnly = true)
    public ChatSummaryDTO riassumi(Chat chat, User io) {
        return riassumi(chat, io, chatRepository.idPreferite(io.getId()).contains(chat.getId()));
    }

    private ChatSummaryDTO riassumi(Chat chat, User io, boolean preferita) {
        String anteprima = messageRepository
                .findByChat_IdOrderBySentAtDesc(chat.getId(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(m -> tronca(m.getContent()))
                .orElse(null);

        return new ChatSummaryDTO(
                chat.getId(),
                UserDTO.from(chat.altroRispettoA(io)),
                anteprima,
                chat.getLastMessageAt(),
                messageRepository.contaNonLetti(chat.getId(), io.getId()),
                preferita);
    }

    /**
     * Carica la chat verificando che chi la chiede ne faccia parte.
     * Senza questo controllo, conoscere un id basterebbe a leggere - o
     * peggio, a scrivere in - la conversazione di due estranei.
     */
    @Transactional(readOnly = true)
    public Chat caricaSePartecipo(UUID chatId, User utente) {
        return caricaPartecipata(chatId, utente);
    }

    private Chat caricaPartecipata(UUID chatId, User utente) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversazione non trovata"));

        if (!chat.partecipa(utente)) {
            throw new AccessDeniedException("Questa conversazione non ti riguarda");
        }
        return chat;
    }

    private String tronca(String testo) {
        return testo.length() <= LUNGHEZZA_ANTEPRIMA
                ? testo
                : testo.substring(0, LUNGHEZZA_ANTEPRIMA) + "…";
    }
}
