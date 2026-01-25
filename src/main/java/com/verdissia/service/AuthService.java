package com.verdissia.service;

import com.verdissia.dto.request.LoginRequest;
import com.verdissia.dto.response.LoginResponse;
import com.verdissia.model.User;
import com.verdissia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour l'utilisateur: {}", request.getUsername());

        try {
            // Recherche de l'utilisateur par username
            User user = userRepository.findByUsername(request.getUsername())
                    .orElse(null);

            // Vérification si l'utilisateur existe et si le mot de passe correspond
            if (user == null) {
                log.warn("Utilisateur non trouvé: {}", request.getUsername());
                return LoginResponse.builder()
                        .message("Données erronées")
                        .success(false)
                        .build();
            }

            if (!user.getActive()) {
                log.warn("Utilisateur désactivé: {}", request.getUsername());
                return LoginResponse.builder()
                        .message("Compte désactivé")
                        .success(false)
                        .build();
            }

            // Vérification stricte du mot de passe
            if (!request.getPassword().equals(user.getPassword())) {
                log.warn("Mot de passe incorrect pour l'utilisateur: {}", request.getUsername());
                return LoginResponse.builder()
                        .message("Données erronées")
                        .success(false)
                        .build();
            }

            // Mise à jour de la date de dernière connexion
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            log.info("Connexion réussie pour l'utilisateur: {}", request.getUsername());

            return LoginResponse.builder()
                    .message("Connection successfully")
                    .username(user.getUsername())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Erreur technique lors de la connexion pour l'utilisateur: {}", request.getUsername(), e);
            throw new RuntimeException("Erreur technique lors de l'authentification", e);
        }
    }
}
