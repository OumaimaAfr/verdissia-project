package com.verdissia.util;

import com.verdissia.constants.ApplicationConstants;
import com.verdissia.dto.response.IResponseDTO;
import com.verdissia.dto.response.ResponseDTO;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@UtilityClass
public class ResponseHelper {

    public static ResponseEntity<IResponseDTO> returnSuccess() {
        return ResponseHelper.returnSuccess(HttpStatus.OK);
    }

    public static ResponseEntity<IResponseDTO> returnSuccess(HttpStatus httpStatus) {
        return ResponseHelper.returnSuccess(httpStatus, null);
    }

    public static <T> ResponseEntity<IResponseDTO> returnSuccess(T data) {
        return ResponseHelper.returnSuccess(HttpStatus.OK, data);
    }

    public static <T> ResponseEntity<IResponseDTO> returnSuccess(HttpStatus httpStatus, T data) {
        ResponseDTO<T> responseDTO = new ResponseDTO<>();
        responseDTO.setStatus(ApplicationConstants.STATUT_OK);
        responseDTO.setData(data);
        return ResponseEntity.status(httpStatus).body(responseDTO);
    }

    public static ResponseEntity<IResponseDTO> returnError(HttpStatusCode httpStatus) {
        return ResponseHelper.returnError(httpStatus, null);
    }

    public static <T> ResponseEntity<IResponseDTO> returnError(
            HttpStatusCode httpStatus, ResponseDTO.CodeMessage error) {
        ResponseDTO<T> responseDTO = new ResponseDTO<>();
        responseDTO.setStatus(ApplicationConstants.STATUT_KO);
        responseDTO.setError(error);
        return ResponseEntity.status(httpStatus).body(responseDTO);
    }

    public static <T> ResponseEntity<IResponseDTO> returnError(
            HttpStatusCode httpStatus, String code, String message) {
        ResponseDTO<T> responseDTO = new ResponseDTO<>();
        responseDTO.setStatus(ApplicationConstants.STATUT_KO);

        ResponseDTO.CodeMessage error = new ResponseDTO.CodeMessage();
        error.setMessage(message);
        error.setCode(code);
        responseDTO.setError(error);
        return ResponseEntity.status(httpStatus).body(responseDTO);
    }
}
