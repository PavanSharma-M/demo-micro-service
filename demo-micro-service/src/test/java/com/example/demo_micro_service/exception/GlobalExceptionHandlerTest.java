package com.example.demo_micro_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ---- ResourceNotFoundException → 404 ----

    @Test
    void handleResourceNotFound_returns404WithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Train not found with id: 99");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Train not found with id: 99");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    // ---- DuplicateResourceException → 409 ----

    @Test
    void handleDuplicateResource_returns409WithMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("Station already exists with code: MMCT");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDuplicateResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Station already exists with code: MMCT");
    }

    // ---- BookingException → 400 ----

    @Test
    void handleBookingException_returns400WithMessage() {
        BookingException ex = new BookingException("Not enough seats available. Available: 1");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBookingException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Not enough seats available. Available: 1");
    }

    // ---- MethodArgumentNotValidException → 400 with validation errors ----

    @Test
    void handleValidationErrors_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("trainDTO", "trainNumber", "Train number is required");

        given(ex.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getAllErrors()).willReturn(List.of(fieldError));

        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().errors()).containsEntry("trainNumber", "Train number is required");
    }

    @Test
    void handleValidationErrors_multipleFields_includesAllErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError err1 = new FieldError("trainDTO", "trainNumber", "Train number is required");
        FieldError err2 = new FieldError("trainDTO", "trainName", "Train name is required");

        given(ex.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getAllErrors()).willReturn(List.of(err1, err2));

        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                handler.handleValidationErrors(ex);

        assertThat(response.getBody().errors()).hasSize(2);
        assertThat(response.getBody().errors()).containsKey("trainNumber");
        assertThat(response.getBody().errors()).containsKey("trainName");
    }

    // ---- Generic Exception → 500 ----

    @Test
    void handleGenericException_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }

    // ---- ErrorResponse record ----

    @Test
    void errorResponseRecord_exposesAllFields() {
        var ts = java.time.LocalDateTime.now();
        GlobalExceptionHandler.ErrorResponse er = new GlobalExceptionHandler.ErrorResponse(404, "Not found", ts);

        assertThat(er.status()).isEqualTo(404);
        assertThat(er.message()).isEqualTo("Not found");
        assertThat(er.timestamp()).isEqualTo(ts);
    }
}
