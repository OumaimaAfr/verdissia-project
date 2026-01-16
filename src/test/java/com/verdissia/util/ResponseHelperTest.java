package com.verdissia.util;

import com.verdissia.constants.ApplicationConstants;
import com.verdissia.dto.response.IResponseDTO;
import com.verdissia.dto.response.ResponseDTO;
import com.verdissia.util.ResponseHelper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseHelperTest {
    @Test
    void testReturnSuccessStringData() {
        ResponseEntity<IResponseDTO> result = ResponseHelper.returnSuccess("data");
        Assertions.assertThat(result).isNotNull();
        ResponseDTO<?> response = (ResponseDTO<?>) result.getBody();
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatus()).isEqualTo(ApplicationConstants.STATUT_OK);
        Assertions.assertThat(response.getData()).isEqualTo("data");
        Assertions.assertThat(response.getError()).isNull();
        Assertions.assertThat(response.getInfo()).isNull();
    }

    @Test
    void testReturnError() {
        ResponseEntity<IResponseDTO> result = ResponseHelper.returnError(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(result).isNotNull();
        ResponseDTO<?> response = (ResponseDTO<?>) result.getBody();
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatus()).isEqualTo(ApplicationConstants.STATUT_KO);
        Assertions.assertThat(response.getData()).isNull();
        Assertions.assertThat(response.getError()).isNull();
        Assertions.assertThat(response.getInfo()).isNull();
    }
}