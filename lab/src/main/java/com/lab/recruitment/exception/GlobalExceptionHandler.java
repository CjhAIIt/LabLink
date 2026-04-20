package com.lab.recruitment.exception;

import com.lab.recruitment.utils.Result;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleSQLIntegrityConstraintViolation(SQLIntegrityConstraintViolationException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, mapIntegrityMessage(e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof SQLIntegrityConstraintViolationException) {
            return handleSQLIntegrityConstraintViolation((SQLIntegrityConstraintViolationException) e.getCause());
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid data");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Object>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Invalid request");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Object>> handleRuntimeException(RuntimeException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private String mapIntegrityMessage(String message) {
        if (message == null) {
            return "Invalid data";
        }

        String normalized = message.toLowerCase();
        if (normalized.contains("cannot be null")) {
            return "Required field is missing";
        }
        if (normalized.contains("duplicate entry") || normalized.contains("duplicate")) {
            if (normalized.contains("student_id") || normalized.contains("uk_user_student_id")
                    || normalized.contains("username")) {
                return "Student ID already registered";
            }
            if (normalized.contains("email")) {
                return "Email already registered";
            }
            if (normalized.contains("phone")) {
                return "Phone number already registered";
            }
            return "Duplicate data";
        }
        if (normalized.contains("student_id") || normalized.contains("uk_user_student_id")
                || normalized.contains("username")) {
            return "Student ID already registered";
        }
        if (normalized.contains("email")) {
            return "Email already registered";
        }
        if (normalized.contains("phone")) {
            return "Phone number already registered";
        }
        return "Invalid data";
    }

    private ResponseEntity<Result<Object>> buildErrorResponse(HttpStatus httpStatus, String message) {
        return ResponseEntity.status(httpStatus).body(Result.error(httpStatus.value(), message));
    }
}
