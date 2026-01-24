package com.verdissia.security.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${users.client-pwd}") String clientPassword,
            @Value("${users.bo-pwd}") String boPassword,
            PasswordEncoder passwordEncoder
    ) {
        UserDetails client = User.withUsername("client")
                .password(passwordEncoder.encode(clientPassword))
                .roles("CLIENT")
                .build();

        UserDetails bo = User.withUsername("bo")
                .password(passwordEncoder.encode(boPassword))
                .roles("BO")
                .build();

        return new InMemoryUserDetailsManager(client, bo);
    }
}
