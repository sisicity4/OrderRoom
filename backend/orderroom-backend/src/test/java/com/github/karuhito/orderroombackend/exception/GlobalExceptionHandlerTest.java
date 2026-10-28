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

    @Test // 精算対象者が0人のとき、400と専用の識別子を返す
    void noSettlementMembersTest() throws Exception {
        NoSettlementMembersException ex = new NoSettlementMembersException(UUID.randomUUID());
        ResponseEntity<ErrorResponse> response = handler.noSettlementMembersException(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("NO_SETTLEMENT_MEMBERS", response.getBody().error());
        assertEquals("精算対象者が0人のため、計算できません", response.getBody().message());
        assertNull(response.getBody().fields());
    }

    @Test // 例外はログで原因を追えるよう、ルームIDを保持する
    void noSettlementMembersExceptionKeepsRoomIdTest() throws Exception {
        UUID roomId = UUID.randomUUID();
        NoSettlementMembersException ex = new NoSettlementMembersException(roomId);

        assertEquals(roomId.toString(), ex.getMessage());
    }

    @Test // 精算済みのため変更できないとき、409と CONFLICT を返す
    void settlementLockedTest() throws Exception {
        SettlementLockedException ex = new SettlementLockedException(UUID.randomUUID());
        ResponseEntity<ErrorResponse> response = handler.settlementLockedException(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("CONFLICT", response.getBody().error());
        assertEquals("精算済みのため、変更できません", response.getBody().message());
        assertNull(response.getBody().fields());
    }

    @Test // 例外はログで原因を追えるよう、ルームIDを保持する
    void settlementLockedExceptionKeepsRoomIdTest() throws Exception {
        UUID roomId = UUID.randomUUID();
        SettlementLockedException ex = new SettlementLockedException(roomId);

        assertEquals(roomId.toString(), ex.getMessage());
    }

    @Test // 送金案が存在しないとき、404と SETTLEMENT_NOT_FOUND を返す
    void settlementNotFoundTest() throws Exception {
        SettlementNotFoundException ex = new SettlementNotFoundException(UUID.randomUUID());
        ResponseEntity<ErrorResponse> response = handler.settlementNotFoundException(ex);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("SETTLEMENT_NOT_FOUND", response.getBody().error());
        assertEquals("送金案が見つかりません", response.getBody().message());
        assertNull(response.getBody().fields());
    }

    @Test // 例外はログで原因を追えるよう、送金案IDを保持する
    void settlementNotFoundExceptionKeepsSettlementIdTest() throws Exception {
        UUID settlementId = UUID.randomUUID();
        SettlementNotFoundException ex = new SettlementNotFoundException(settlementId);

        assertEquals(settlementId.toString(), ex.getMessage());
    }

    @Test // 支払側本人でもホストでもないとき、403と FORBIDDEN を返す
    void notSettlementPayerTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        NotSettlementPayerException ex = new NotSettlementPayerException(UUID.randomUUID(), UUID.randomUUID());
        ResponseEntity<ErrorResponse> response = handler.notSettlementPayerException(ex, request);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().error());
        assertEquals("送金案を操作する権限がありません", response.getBody().message());
        assertNull(response.getBody().fields());
    }

    @Test // 例外はログで原因を追えるよう、参加者IDと送金案IDを保持する
    void notSettlementPayerExceptionKeepsIdsTest() throws Exception {
        UUID participantId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();
        NotSettlementPayerException ex = new NotSettlementPayerException(participantId, settlementId);

        assertEquals(participantId.toString(), ex.getMessage());
        assertEquals(settlementId, ex.getSettlementId());
    }
}