package com.github.karuhito.orderroombackend.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.karuhito.orderroombackend.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidException(MethodArgumentNotValidException ex) {
            Map<String, String> fieldsMap = new HashMap<>();
            for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
                fieldsMap.put(fieldError.getField(), fieldError.getDefaultMessage());
            }

            ErrorResponse response = new ErrorResponse("VALID_ERROR", "不正な入力です", fieldsMap);
            return ResponseEntity.status(400).body(response);
        }   
}