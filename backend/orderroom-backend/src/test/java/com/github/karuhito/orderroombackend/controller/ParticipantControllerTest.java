package com.github.karuhito.orderroombackend.controller;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.test.web.servlet.MockMvc;


import com.github.karuhito.orderroombackend.dto.CreateParticipantRequest;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc

public class ParticipantControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Test // 正しい場合
    void createParticipant() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        CreateParticipantRequest request = new CreateParticipantRequest("テスト");

        mockMvc.perform(
            post("/api/rooms/{roomId}/participants", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("テスト"))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.token").exists())
        .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test // 1.roomIDが存在しないUUIDの場合
    void createParticipantNotFoundRoomId() throws Exception {
        UUID roomId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        CreateParticipantRequest request = new CreateParticipantRequest("テスト");

        mockMvc.perform(
            post("/api/rooms/{roomId}/participants", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("ルームが見つかりません"));
    }

    @Test // 2.型が不一致の場合
    void createParticipantInvalidRoomIdType() throws Exception {
        // roomIdがString型
        String roomId = "1d3afab3-8537-4275-afaa-c4268211a2xx";
        CreateParticipantRequest request = new CreateParticipantRequest("テスト");

        mockMvc.perform(
            post("/api/rooms/{roomId}/participants", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
        .andExpect(jsonPath("$.fields.roomId").value("不正な値: " + roomId));
    }


    @Test // 3.1 Participantのnameが空文字の場合
    void createParticipantNotParticipantName() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        CreateParticipantRequest request = new CreateParticipantRequest(""); // 空文字

        mockMvc.perform(
            post("/api/rooms/{roomId}/participants", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("名前を正しく入力してください"));
    }

    @Test // 3.2 Participantのnameが空文字の場合
    void createParticipantNullParticipantName() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        CreateParticipantRequest request = new CreateParticipantRequest(null); // nameがnull

        mockMvc.perform(
            post("/api/rooms/{roomId}/participants", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("名前を正しく入力してください"));
    }
}