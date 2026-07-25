package com.github.karuhito.orderroombackend.interceptor;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import com.github.karuhito.orderroombackend.entity.Room;

import com.github.karuhito.orderroombackend.exception.InvalidHostKeyException;
import com.github.karuhito.orderroombackend.exception.InvalidHostKeyReason;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;

import com.github.karuhito.orderroombackend.repository.RoomRepository;




@SpringBootTest
public class HostKeyInterceptorTest {
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HostKeyInterceptor hostKeyInterceptor;

    @Test // 正常系(正しいhost_keyの場合)
    void preHandleValidHostKey() throws Exception {
        Room room = new Room("テストタイトル");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Host-Key", hostKey.toString());
        
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of( "roomId", roomId.toString()));
        boolean result = assertDoesNotThrow(() -> hostKeyInterceptor.preHandle(request, null, null));
        assertTrue(result);   
    }

    @Test // 異常系 1.ホストキーが欠如
    void preHandleMissingHostKey() throws Exception {
        Room room = new Room("テストタイトル");
        roomRepository.save(room);
        UUID roomId = room.getId();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of( "roomId", roomId.toString()));

        InvalidHostKeyException exception = assertThrows(InvalidHostKeyException.class, () -> 
        hostKeyInterceptor.preHandle(request, null, null));
        assertEquals( InvalidHostKeyReason.MISSING, exception.getReason());
    }
    @Test // 2.ホストキーが不一致
    void preHandleMismatchHostKey() throws Exception {
        Room room = new Room("テストタイトル");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = UUID.randomUUID();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Host-Key", hostKey.toString());
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of( "roomId", roomId.toString()));

        InvalidHostKeyException exception = assertThrows(InvalidHostKeyException.class, () ->
        hostKeyInterceptor.preHandle(request, null, null));
        assertEquals(InvalidHostKeyReason.MISMATCH, exception.getReason());
    }

    @Test // 3.存在しないルームIDの場合
    void preHandleRoomNotFound() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID hostKey = UUID.randomUUID();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Host-Key", hostKey.toString());
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of( "roomId", roomId.toString()));

        RoomNotFoundException exception = assertThrows(RoomNotFoundException.class, () ->
        hostKeyInterceptor.preHandle(request, null, null));
        assertEquals(roomId.toString(), exception.getMessage());
    }
}