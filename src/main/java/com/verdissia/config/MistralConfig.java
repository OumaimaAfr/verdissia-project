package com.verdissia.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mistral.api")
@Data
public class MistralConfig {
    private String url;
    private String key;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}