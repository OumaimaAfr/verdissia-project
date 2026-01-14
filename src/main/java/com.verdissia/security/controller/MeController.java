package com.verdissia.security.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication auth){
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;

        return Map.of(
                "userId", jwt.getToken().getSubject(),
                "username", jwt.getToken().getClaimAsString("preferred_username"),
                "roles", jwt.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList())
        );
    }
}
