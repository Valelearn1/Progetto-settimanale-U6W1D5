package com.example.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String mittente;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendRegistrationEmail(String destinatario, String displayName, String codice) {
        String link = "%s/verifica?email=%s&codice=%s".formatted(
                frontendUrl,
                URLEncoder.encode(destinatario, StandardCharsets.UTF_8),
                URLEncoder.encode(codice, StandardCharsets.UTF_8));

        invia(destinatario,
                "Conferma la tua registrazione a Sinapsi",
                "mail/registrazione",
                Map.of("nome", displayName, "codice", codice, "link", link),
                "Attiva il tuo account Sinapsi",
                """
                Ciao %s, benvenuto su Sinapsi.

                Per attivare l'account apri questo link (valido 24 ore):
                %s

                Oppure inserisci questo codice nella pagina di verifica:
                %s

                Se non ti sei registrato tu, ignora questo messaggio.
                """.formatted(displayName, link, codice));
    }

    /**
     * Costruisce e invia il messaggio.
     *
     * <p>Vengono incluse DUE versioni dello stesso contenuto
     * (multipart/alternative): quella HTML e quella di solo testo. I client che
     * non mostrano l'HTML usano la seconda, e avere entrambe riduce le
     * probabilita' di finire nello spam.
     */
    private void invia(String destinatario, String oggetto, String template,
                       Map<String, Object> variabili, String anteprima, String testoSemplice) {
        try {
            Context context = new Context();
            context.setVariables(variabili);
            context.setVariable("titolo", oggetto);
            context.setVariable("anteprima", anteprima);
            // Il template riempie il frammento "contenuto" dentro il layout comune.
            context.setVariable("contenuto", template + " :: contenuto");

            String html = templateEngine.process("mail/layout", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mittente);
            helper.setTo(destinatario);
            helper.setSubject(oggetto);
            helper.setText(testoSemplice, html);

            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new IllegalStateException("Invio email non riuscito: " + e.getMessage(), e);
        }
    }
}
