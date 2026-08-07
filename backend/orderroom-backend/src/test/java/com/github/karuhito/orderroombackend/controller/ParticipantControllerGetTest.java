package com.github.karuhito.orderroombackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;


@SpringBootTest
@AutoConfigureMockMvc

public class ParticipantControllerGetTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParticipantRepository participantRepository;
    
    @Test // 正常系 
    void getParticipants() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        Participant participant1 = new Participant(room, "テスト参加者1");
        participantRepository.save(participant1);

        Participant participant2 = new Participant(room, "テスト参加者2");
        participantRepository.save(participant2);
        
        mockMvc.perform(
            get("/api/rooms/{roomId}/participants", roomId)
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("テスト参加者1"))
        .andExpect(jsonPath("$[1].name").value("テスト参加者2"))
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[1].id").exists())
        .andExpect(jsonPath("$[0].createdAt").exists())
        .andExpect(jsonPath("$[0].token").doesNotExist());
    }

    // 異常系
    @Test // 1: 存在しないroomId
    void getParticipantsNotFoundRoom() throws Exception {
        UUID roomId = UUID.randomUUID();

        mockMvc.perform(
            get("/api/rooms/{roomId}/participants", roomId)
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("ルームが見つかりません"));
    }

    @Test // 2: 参加者が一人もいないroomに取得を行おうとした時
    void getParticipantsEmptyList() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();


        mockMvc.perform(
            get("/api/rooms/{roomId}/participants", roomId)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    }

    @Test // 3.roomIdの型が不一致の場合
    void getParticipantsInvalidRoomIdType() throws Exception {
        String roomId = "1d3afab3-8537-4275-afaa-c4268211a2xx";

        mockMvc.perform(
            get("/api/rooms/{roomId}/participants", roomId)
            .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
        .andExpect(jsonPath("$.fields.roomId").value("不正な値: " + roomId));
    }
}