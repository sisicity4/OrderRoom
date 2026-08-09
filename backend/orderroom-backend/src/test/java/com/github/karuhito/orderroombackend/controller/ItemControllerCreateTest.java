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
public class ItemControllerCreateTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    /**
    * CreateItemのテスト
    */
    @Test // 正常系
    void createItem() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
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
        .andExpect(jsonPath("$.status").value("proposed"))
        .andExpect(jsonPath("$.purchased").value(false))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
    }

    // 異常系
    // 1. トークン検証(403)
    @Test // 1-1 トークンが欠如している
    void createItemMissingToken() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("トークンが無効です"));
    }

    @Test // 1-2 トークンの形式が不正(UUIDとして解釈できない)
    void createItemInvalidFormatToken() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        String token = "undefined";

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("トークンが無効です"));
    }

    @Test // 1-3 どの参加者とも一致しないtoken
    void createItemMismatchToken() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        UUID token = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("トークンが無効です"));
    }

    @Test // 1-4 別ルームの参加者のtoken
    void createItemOtherRoomToken() throws Exception {
        Room room1 = new Room("テストルーム1");
        roomRepository.save(room1);

        Room room2 = new Room("テストルーム2");
        roomRepository.save(room2);

        UUID roomId = room2.getId();

        Participant participant = new Participant(room1, "別ルームの参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("トークンが無効です"));
    }

    @Test // 1-5 roomIdが実在しない(tokenを照合できず403)
    void createItemUnknownRoom() throws Exception {
        UUID roomId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        UUID token = UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("トークンが無効です"));
    }

    @Test // 1-6 幹事(hostKey保持者)であってもtokenが無ければ提案できない(設計書4.3)
    void createItemHostKeyMissingToken() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("トークンが無効です"));
    }

    // 2. 入力バリデーション(400)
    @Test // 2-1-1 Itemのnameが空文字の場合
    void createItemNotName() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest("", 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("アイテム名を正しく入力してください"));
    }

    @Test // 2-1-2 Itemのnameがnullの場合
    void createItemNullName() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest(null, 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("アイテム名を正しく入力してください"));
    }

    @Test // 2-1-3 Itemのnameが最大文字数を超える場合
    void createItemNameLengthInValidName() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest("あ".repeat(101), 2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("アイテム名は100字以内で入力してください"));
    }

    @Test // 2-2-1 Itemのpriceがマイナスの場合
    void createItemInValidPrice() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", -2000, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.price").value("値段をマイナスに設定することはできません"));
    }

    @Test // 2-2-2 Itemのpriceがnullの場合
    void createItemNullPrice() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", null, 1, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.price").value("値段を正しく入力してください"));
    }

    @Test // 2-3-1 Itemのquantityが0以下の場合
    void createItemInValidQuantity() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, 0, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.quantity").value("個数を0以下に設定することはできません"));
    }

    @Test // 2-3-2 Itemのquantityがnullの場合
    void createItemNullQuantity() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        CreateItemRequest request = new CreateItemRequest("テストアイテム", 2000, null, "テストメモ");
        mockMvc.perform(
            post("/api/rooms/{roomId}/items", roomId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.quantity").value("個数を正しく入力してください"));
    }

}
