package com.github.karuhito.orderroombackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class ItemControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Test // 正しい場合
    void CreateItem() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();

        CreateItemRequest request = new CreateItemRequest(participantId, "テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.participantId").value(participantId.toString()))
        .andExpect(jsonPath("$.name").value("テストアイテム"))
        .andExpect(jsonPath("$.price").value(2000))
        .andExpect(jsonPath("$.quantity").value(1))
        .andExpect(jsonPath("$.status").value("PROPOSED"))
        .andExpect(jsonPath("$.purchased").value(false))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
        
    }

    @Test // 1. roomIdが実在しない
    void CreateItemNotFoundRoom() throws Exception {
        UUID roomId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, "テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("ルームが見つかりません"));
    }
    
    @Test // 2. 1roomIdは実在するがparticipantIdが実在しない
    void CreateItemInValidParticipantId() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, "テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("PARTICIPANT_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("参加者IDが正しくありません"));
    }

    @Test // 3. participantIDが別のroomと紐づいている場合
    void CreateItemNotFoundParticipantId() throws Exception {
        Room room1 = new Room("テストルーム1");
        roomRepository.save(room1);
        
        Room room2 = new Room("テストルーム2");
        roomRepository.save(room2);
        
        UUID roomId = room2.getId();

        Participant participant = new Participant(room1, "別ルームの参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();

        CreateItemRequest request = new CreateItemRequest(participantId, "テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("PARTICIPANT_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("参加者IDが正しくありません"));
    }

    @Test // 4.1 Itemのnameが空文字の場合
    void createItemNotName() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, "", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("アイテム名を正しく入力してください"));
    }

    @Test // 4.2 Itemのnameがnullの場合
    void createItemNullName() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, null, 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("アイテム名を正しく入力してください"));
    }

    @Test // 4.3 Itemのnameが最大文字数を超える場合
    void createItemNameLengthInValidName() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, "あ".repeat(101), 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("アイテム名は100字以内で入力してください"));
    }
    
    @Test // 5.1 Itemのpriceがマイナスの場合
    void createItemInValidPrice() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, "テストアイテム", -2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.price").value("値段をマイナスに設定することはできません"));
    }

    @Test // 5.2 Itemのpriceがnullの場合
    void createItemNullPrice() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, "テストアイテム", null, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.price").value("値段を正しく入力してください"));
    }

    
    @Test // 6.1 Itemのquantityが0以下の場合
    void createItemInValidQuantity() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, "テストアイテム", 2000, 0, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.quantity").value("個数を0以下に設定することはできません"));
    }

    @Test // 6.2 Itemのquantityがnullの場合
    void createItemNullQuantity() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(participantId, "テストアイテム", 2000, null, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.quantity").value("個数を正しく入力してください"));
    }
}