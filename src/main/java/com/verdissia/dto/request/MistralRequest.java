package com.verdissia.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class MistralRequest {
    private String model;
    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}

