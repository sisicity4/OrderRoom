package com.github.karuhito.orderroombackend.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.github.karuhito.orderroombackend.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

        @ExceptionHandler(InvalidHostKeyException.class)
        public ResponseEntity<ErrorResponse> invalidHostKeyException(InvalidHostKeyException ex) {
            ErrorResponse response = new ErrorResponse("FORBIDDEN", "ホストキーが無効です", null);
            log.warn("ルーム: {} で {} が起きています", ex.getMessage(), ex.getReason());
            return ResponseEntity.status(403).body(response);
        }

        @ExceptionHandler(ItemNotFoundException.class)
        public ResponseEntity<ErrorResponse> itemNotFoundException(ItemNotFoundException ex) {
            ErrorResponse response = new ErrorResponse("ITEM_NOT_FOUND", "アイテムが見つかりません", null);
            return ResponseEntity.status(404).body(response);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
            Map<String, String> fieldsMap = new HashMap<>();
            fieldsMap.put("status", "不正なstatusです");
            ErrorResponse response = new ErrorResponse("TYPE_MISMATCH", "パラメータの型が不正です", fieldsMap);
            return ResponseEntity.status(400).body(response);
        }
        
        @ExceptionHandler(ItemStatusInvalidException.class)
        public ResponseEntity<ErrorResponse> itemStatusInValidException(ItemStatusInvalidException ex) {
            ErrorResponse response  = new ErrorResponse("CONFLICT", "アイテムを採用済みにしている必要があります", null);
            return ResponseEntity.status(409).body(response);
        }
}