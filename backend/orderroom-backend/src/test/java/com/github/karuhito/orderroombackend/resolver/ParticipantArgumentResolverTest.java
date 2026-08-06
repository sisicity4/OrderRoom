package com.github.karuhito.orderroombackend.resolver;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;

import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.exception.InvalidTokenException;
import com.github.karuhito.orderroombackend.exception.InvalidTokenReason;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ParticipantArgumentResolverTest {
    @Autowired
    RoomRepository roomRepository;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    ParticipantArgumentResolver participantArgumentResolver;
    
    // 正常系
    @Test
    void resolveArgumentValidToken() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("X-Participant-Token", token.toString());
        mockHttpServletRequest.setAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Map.of("roomId", roomId.toString())
        );
        ServletWebRequest servletWebRequest = new ServletWebRequest(mockHttpServletRequest);
        Object result = participantArgumentResolver.resolveArgument(null, null, servletWebRequest, null);
        Participant resultParticipant = (Participant)result;
        assertEquals(participant.getId(), resultParticipant.getId());        
    }

    // 異常系
    @Test // 1. トークンが欠如
    void resolveArgumentMissingToken() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Map.of("roomId", roomId.toString())
        );
        ServletWebRequest servletWebRequest = new ServletWebRequest(mockHttpServletRequest);

        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () ->
            participantArgumentResolver.resolveArgument(null,null, servletWebRequest, null));
        assertEquals(InvalidTokenReason.MISSING, exception.getReason());
    }

    @Test // 2. トークンの形式が不正(明らかにUUIDではない)
    void resolveArgumentInvalidFormatToken() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        String token = "undefined";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("X-Participant-Token", token);
        mockHttpServletRequest.setAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Map.of("roomId", roomId.toString())
        );
        ServletWebRequest servletWebRequest = new ServletWebRequest(mockHttpServletRequest);

        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () ->
            participantArgumentResolver.resolveArgument(null,null, servletWebRequest, null));
        assertEquals(InvalidTokenReason.INVALID_FORMAT, exception.getReason());
    }

    @Test // 3. 存在しないトークン
    void resolveArgumentMismatchToken() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        UUID token = UUID.randomUUID();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("X-Participant-Token", token.toString());
        mockHttpServletRequest.setAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Map.of("roomId", roomId.toString())
        );
        ServletWebRequest servletWebRequest = new ServletWebRequest(mockHttpServletRequest);

        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () ->
            participantArgumentResolver.resolveArgument(null,null, servletWebRequest, null));
        assertEquals(InvalidTokenReason.MISMATCH, exception.getReason());
    }

    @Test // 4. 別ルームのトークン
    void resolveArgumentOtherRoomToken() throws Exception {
        Room room1 = new Room("テストルーム1");
        roomRepository.save(room1);
        UUID roomId = room1.getId();

        Room room2 = new Room("テストルーム2");
        roomRepository.save(room2);
        
        Participant participant = new Participant(room2, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("X-Participant-Token", token.toString());
        mockHttpServletRequest.setAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            Map.of("roomId", roomId.toString())
        );
        ServletWebRequest servletWebRequest = new ServletWebRequest(mockHttpServletRequest);

        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () ->
            participantArgumentResolver.resolveArgument(null,null, servletWebRequest, null));
        assertEquals(InvalidTokenReason.MISMATCH, exception.getReason());
    }



}