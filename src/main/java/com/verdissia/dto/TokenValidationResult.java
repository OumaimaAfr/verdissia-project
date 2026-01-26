package com.verdissia.dto;

import com.verdissia.model.SignatureToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResult {
    private boolean valid;
    private ValidationStatus status;
    private SignatureToken token;
    private String message;

    public enum ValidationStatus {
        VALID,
        TOKEN_NOT_FOUND,
        TOKEN_EXPIRED,
        TOKEN_ALREADY_USED,
        TOKEN_INVALID_STATUS
    }

    public static TokenValidationResult valid(SignatureToken token) {
        return TokenValidationResult.builder()
                .valid(true)
                .status(ValidationStatus.VALID)
                .token(token)
                .build();
    }

    public static TokenValidationResult invalid(ValidationStatus status, String message) {
        return TokenValidationResult.builder()
                .valid(false)
                .status(status)
                .message(message)
                .build();
    }
}
