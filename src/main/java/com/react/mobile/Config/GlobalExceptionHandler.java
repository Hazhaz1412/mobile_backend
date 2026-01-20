package com.react.mobile.Config;

import com.react.mobile.DTO.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${app.debug:false}")
    private boolean debugMode;

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.NOT_FOUND,
            "Resource Not Found",
            debugMode ? ex.getMessage() : "The requested resource was not found",
            request.getRequestURI(),
            ex
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.NOT_FOUND,
            "Not Found",
            debugMode ? ex.getMessage() : "The requested endpoint does not exist",
            request.getRequestURI(),
            ex
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            details.put(error.getField(), error.getDefaultMessage())
        );
        
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            debugMode ? "Validation error on request" : "Invalid request data",
            request.getRequestURI(),
            ex,
            details
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            debugMode ? ex.getMessage() : "Invalid parameter type",
            request.getRequestURI(),
            ex
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "Invalid username or password",
            request.getRequestURI(),
            ex
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            debugMode ? ex.getMessage() : "Authentication failed",
            request.getRequestURI(),
            ex
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            debugMode ? ex.getMessage() : "Access denied",
            request.getRequestURI(),
            ex
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            ex
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception occurred", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            debugMode ? ex.getMessage() : "An unexpected error occurred",
            request.getRequestURI(),
            ex
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception occurred", ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            debugMode ? ex.getMessage() : "An error occurred while processing your request",
            request.getRequestURI(),
            ex
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, 
            String error, 
            String message, 
            String path, 
            Exception ex) {
        return buildErrorResponse(status, error, message, path, ex, null);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, 
            String error, 
            String message, 
            String path, 
            Exception ex,
            Map<String, Object> additionalDetails) {
        
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path);
        
        // Nếu DEBUG = true, thêm chi tiết exception
        if (debugMode && ex != null) {
            builder.exception(ex.getClass().getName());
            builder.trace(getStackTrace(ex));
            
            if (additionalDetails != null) {
                builder.details(additionalDetails);
            }
        }
        
        return ResponseEntity.status(status).body(builder.build());
    }

    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}
