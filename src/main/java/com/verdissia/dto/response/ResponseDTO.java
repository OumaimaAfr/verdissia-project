package com.verdissia.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ResponseDTO<T> implements IResponseDTO {

    @JsonProperty("statut")
    private String status;

    @JsonProperty("data")
    private T data;

    @JsonProperty("erreur")
    private CodeMessage error;

    @JsonProperty("info")
    private CodeMessage info;

    @Getter
    @Setter
    public static class CodeMessage {
        private String code;
        private String message;
    }
}