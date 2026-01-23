package com.verdissia.dto.response;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ResponseDTO<T> implements IResponseDTO {

    private String status;

    private T data;

    private CodeMessage error;

    private CodeMessage info;

    @Getter
    @Setter
    public static class CodeMessage {
        private String code;
        private String message;
    }
}