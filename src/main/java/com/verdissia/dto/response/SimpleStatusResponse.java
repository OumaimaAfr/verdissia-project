package com.verdissia.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties({"data", "info"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleStatusResponse implements IResponseDTO<Void> {

    private String status;
    private String message;

    public SimpleStatusResponse(String status) {
        this.status = status;
    }

    @Override
    public Void getData() {
        return null;
    }

    @Override
    public ResponseDTO.CodeMessage getError() {
        return null;
    }

    @Override
    public ResponseDTO.CodeMessage getInfo() {
        return null;
    }
}
