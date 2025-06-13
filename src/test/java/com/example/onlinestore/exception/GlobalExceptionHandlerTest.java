package com.example.onlinestore.exception;

import com.example.onlinestore.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private ServletWebRequest webRequest;
    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private ConstraintViolationException constraintViolationException;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private final String TEST_URI = "/test/uri";

    @BeforeEach
    void setUp() {
        when(webRequest.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getRequestURI()).thenReturn(TEST_URI);
    }

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Test illegal argument");
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleIllegalArgumentException(ex, webRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Test illegal argument", errorResponse.getMessage());
        assertEquals(TEST_URI, errorResponse.getPath());
    }

    @Test
    void handleMethodArgumentNotValid() {
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("objectName", "fieldName", "default message")));

        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleMethodArgumentNotValid(methodArgumentNotValidException, webRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Validation Failed", errorResponse.getError());
        assertEquals("fieldName: default message", errorResponse.getMessage());
        assertEquals(TEST_URI, errorResponse.getPath());
    }

    @Test
    void handleConstraintViolationException() {
        // Mock ConstraintViolation
        ConstraintViolation<?> mockViolation = org.mockito.Mockito.mock(ConstraintViolation.class);
        when(mockViolation.getPropertyPath()).thenReturn(jakarta.validation.Path.PathImpl.createPathFromString("paramName"));
        when(mockViolation.getMessage()).thenReturn("must be valid");

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(mockViolation);
        when(constraintViolationException.getConstraintViolations()).thenReturn(violations);

        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleConstraintViolationException(constraintViolationException, webRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Validation Failed", errorResponse.getError());
        assertTrue(errorResponse.getMessage().contains("paramName: must be valid"));
        assertEquals(TEST_URI, errorResponse.getPath());
    }

    @Test
    void handleUnauthorizedException() {
        UnauthorizedException ex = new UnauthorizedException("Test unauthorized access");
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleUnauthorizedException(ex, webRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), errorResponse.getStatus());
        assertEquals("Unauthorized", errorResponse.getError());
        assertEquals("Test unauthorized access", errorResponse.getMessage());
        assertEquals(TEST_URI, errorResponse.getPath());
    }

    @Test
    void handleGenericRuntimeException() {
        RuntimeException ex = new RuntimeException("Test generic runtime error");
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleGenericRuntimeException(ex, webRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Internal Server Error", errorResponse.getError());
        assertTrue(errorResponse.getMessage().contains("Test generic runtime error"));
        assertEquals(TEST_URI, errorResponse.getPath());
    }

    @Test
    void handleAllUncaughtException() {
        Exception ex = new Exception("Test general error");
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleAllUncaughtException(ex, webRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("An unexpected server error occurred.", errorResponse.getMessage());
        assertEquals(TEST_URI, errorResponse.getPath());
    }
}
