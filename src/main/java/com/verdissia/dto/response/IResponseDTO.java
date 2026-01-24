package com.verdissia.dto.response;

public interface IResponseDTO<T> {
    String getStatus();
    T getData();
    ResponseDTO.CodeMessage getError();
    ResponseDTO.CodeMessage getInfo();
}
