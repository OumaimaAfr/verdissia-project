package com.verdissia.controller;

import com.verdissia.dto.request.LoginRequest;
import com.verdissia.dto.response.LoginResponse;
import com.verdissia.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verdisia/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthVerdissiaController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Requête de connexion reçue pour l'utilisateur: {}", loginRequest.getUsername());

        try {
            LoginResponse response = authService.login(loginRequest);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (RuntimeException e) {
            log.error("Erreur lors de la connexion pour l'utilisateur: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.builder()
                            .message("Erreur technique")
                            .success(false)
                            .build());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la connexion pour l'utilisateur: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.builder()
                            .message("Erreur technique")
                            .success(false)
                            .build());
        }
    }
}
