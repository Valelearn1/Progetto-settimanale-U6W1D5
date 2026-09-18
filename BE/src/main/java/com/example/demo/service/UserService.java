package com.example.demo.service;

import com.example.demo.dto.response.UserDTO;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    /**
     * La rubrica, escluso chi la sta chiedendo: nessuno ha bisogno di vedere
     * se stesso fra i contatti con cui puo' aprire una conversazione.
     */
    @Transactional(readOnly = true)
    public List<UserDTO> rubricaPer(User richiedente) {
        return repository.findByActiveTrueOrderByDisplayNameAsc().stream()
                .filter(u -> !u.getId().equals(richiedente.getId()))
                .map(UserDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato: " + id));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato: " + username));
    }
}
