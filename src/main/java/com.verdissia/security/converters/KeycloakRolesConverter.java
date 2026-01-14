package com.verdissia.security.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String clientId; // optionnel

    public KeycloakRolesConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new HashSet<>();

// 1) realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object rs = realmAccess.get("roles");
            if (rs instanceof Collection<?> col) {
                col.forEach(r -> roles.add(String.valueOf(r)));
            }
        }

// 2) client roles (optionnel)
        if (clientId != null && !clientId.isBlank()) {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                Object client = resourceAccess.get(clientId);
                if (client instanceof Map<?, ?> clientMap) {
                    Object cr = clientMap.get("roles");
                    if (cr instanceof Collection<?> col) {
                        col.forEach(r -> roles.add(String.valueOf(r)));
                    }
                }
            }
        }

// Convert to Spring authorities (ROLE_*)
        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}