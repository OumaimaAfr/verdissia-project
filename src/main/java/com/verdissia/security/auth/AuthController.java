package com.verdissia.security.auth;

import com.verdissia.security.dto.LoginRequest;
import com.verdissia.security.dto.TokenResponse;
import com.verdissia.security.jwt.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/verdisia")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/generation-token")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String rawPassword;
        try {
            rawPassword = new String(java.util.Base64.getDecoder().decode(loginRequest.getPassword()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Mot de passe doit être encodé en Base64");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), rawPassword)
        );

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = jwtService.generate(authentication.getName(), Map.of("roles", roles));

        return ResponseEntity.ok(new TokenResponse(token, "Bearer"));
    }
}
