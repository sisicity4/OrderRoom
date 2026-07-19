package com.github.karuhito.orderroombackend.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

        @ExceptionHandler(RoomNotFoundException.class)
        public ResponseEntity<ErrorResponse> roomNotFoundException(RoomNotFoundException ex){
            ErrorResponse response = new ErrorResponse("ROOM_NOT_FOUND", "ルームが見つかりません", null);
            return ResponseEntity.status(404).body(response);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
            Map<String, String> fieldsMap = new HashMap<>();
            fieldsMap.put(ex.getName(), "不正な値: " + ex.getValue());

            ErrorResponse response = new ErrorResponse("TYPE_MISMATCH", "パラメータの型が不正です", fieldsMap);
            return ResponseEntity.status(400).body(response);
        }

        @ExceptionHandler(ParticipantNotFoundException.class)
        public ResponseEntity<ErrorResponse> participantNotFoundException(ParticipantNotFoundException ex) {
            ErrorResponse response = new ErrorResponse("PARTICIPANT_NOT_FOUND", "参加者IDが正しくありません", null);
            return ResponseEntity.status(404).body(response);
        }
}