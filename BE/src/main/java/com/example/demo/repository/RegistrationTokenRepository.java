package com.example.demo.repository;

import com.example.demo.entity.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, UUID> {

    /**
     * Il codice va cercato insieme all'utente a cui appartiene: senza il
     * vincolo sull'email, un codice valido di un utente attiverebbe l'account
     * di chiunque altro.
     */
    Optional<RegistrationToken> findByValueAndUser_Email(String value, String email);
}
