package io.github.smling.proxmoxmcpserver.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTests {

    @Test
    void handleBadRequestReturns400() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ResponseEntity<String> response = handler.handleBadRequest(new IllegalArgumentException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("bad");
    }

    @Test
    void handleServerErrorReturns500() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ResponseEntity<String> response = handler.handleServerError(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("boom");
    }
}
