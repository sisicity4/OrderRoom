package com.github.karuhito.orderroombackend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

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

    @Test // 購入済みアイテム詳細でアイテムのstatusがAcceptedでない時
    void invalidPurchaseDetailStateNotAcceptedTest() throws Exception {
        InvalidPurchaseDetailStateException ex = new InvalidPurchaseDetailStateException(
            UUID.randomUUID(),
            InvalidPurchaseDetailStateReason.NOT_ACCEPTED
        );
        ResponseEntity<ErrorResponse> response = handler.invalidPurchaseDetailStateException(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ITEM_STATE", response.getBody().error());
        assertEquals("アイテムを採用済みにしている必要があります", response.getBody().message());
        assertNull(response.getBody().fields());
    }

    @Test // 購入済みアイテムのpurchasedがfalseの場合
    void invalidPurchaseDetailStateNotPurchasedTest() throws Exception {
        InvalidPurchaseDetailStateException ex = new InvalidPurchaseDetailStateException(
            UUID.randomUUID(),
            InvalidPurchaseDetailStateReason.NOT_PURCHASED
        );
        ResponseEntity<ErrorResponse> response = handler.invalidPurchaseDetailStateException(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ITEM_STATE", response.getBody().error());
        assertEquals("アイテムを購入済みにしている必要があります", response.getBody().message());
        assertNull(response.getBody().fields());
    }
}