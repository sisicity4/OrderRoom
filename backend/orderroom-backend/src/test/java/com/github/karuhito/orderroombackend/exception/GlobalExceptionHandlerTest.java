package com.github.karuhito.orderroombackend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;


import com.github.karuhito.orderroombackend.dto.ErrorResponse;

public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    
    @Test // 500のハンドラが正しいかを確認するテスト 
    void handleUnexpectedExceptionTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RuntimeException ex = new RuntimeException("テスト用例外");
        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(ex, request);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().error());
        assertEquals("サーバーエラーが発生しました", response.getBody().message());
        assertNull(response.getBody().fields());
    }   
}