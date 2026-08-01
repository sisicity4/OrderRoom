package com.github.karuhito.orderroombackend.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.karuhito.orderroombackend.dto.UpdateItemStatusRequest;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class ItemControllerUpdateStatusTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ItemRepository itemRepository;
    

    /**
     * *  UpdateStatusのテスト
    */
    // 正常系1: statusが更新され、200と更新後のアイテムが返ること（proposed → accepted、proposed → rejected）
    // 正常系1.1: proposed -> accepted に更新する場合
    @Test
    void updateStatusToAccepted() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.PROPOSED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.ACCEPTED);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.participantId").value(participantId.toString()))
        .andExpect(jsonPath("$.participantName").value(participant.getName()))
        .andExpect(jsonPath("$.name").value("テストアイテム"))
        .andExpect(jsonPath("$.price").value(100))
        .andExpect(jsonPath("$.quantity").value(1))
        .andExpect(jsonPath("$.memo").value(nullValue()))
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.purchased").value(false))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
    }
    // 正常系1.2: proposed → rejected
    @Test
    void updateStatusToRejected() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.PROPOSED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.REJECTED);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.participantId").value(participantId.toString()))
        .andExpect(jsonPath("$.participantName").value(participant.getName()))
        .andExpect(jsonPath("$.name").value("テストアイテム"))
        .andExpect(jsonPath("$.price").value(100))
        .andExpect(jsonPath("$.quantity").value(1))
        .andExpect(jsonPath("$.memo").value(nullValue()))
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.purchased").value(false))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
    }

    // 正常系2: 遷移制限がないこと（accepted → proposed、rejected → accepted）
    // 正常系2.1: accepted → proposedの場合
    @Test
    void updateStatusFromAcceptedToProposed() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.PROPOSED);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.participantId").value(participantId.toString()))
        .andExpect(jsonPath("$.participantName").value(participant.getName()))
        .andExpect(jsonPath("$.name").value("テストアイテム"))
        .andExpect(jsonPath("$.price").value(100))
        .andExpect(jsonPath("$.quantity").value(1))
        .andExpect(jsonPath("$.memo").value(nullValue()))
        .andExpect(jsonPath("$.status").value("PROPOSED"))
        .andExpect(jsonPath("$.purchased").value(false))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
    }

    // 正常系2.2: rejected → acceptedの場合
    @Test
    void updateStatusFromRejectedToAccepted() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.REJECTED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.ACCEPTED);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.participantId").value(participantId.toString()))
        .andExpect(jsonPath("$.participantName").value(participant.getName()))
        .andExpect(jsonPath("$.name").value("テストアイテム"))
        .andExpect(jsonPath("$.price").value(100))
        .andExpect(jsonPath("$.quantity").value(1))
        .andExpect(jsonPath("$.memo").value(nullValue()))
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.purchased").value(false))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
    }
    // 異常系1: X-Host-Key欠如 → 403 FORBIDDEN（interceptor経由）
    @Test
    void updateStatusMissingHostKey() throws Exception{
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        
        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.PROPOSED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.ACCEPTED);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("ホストキーが無効です"));
    }

    // 異常系2: X-Host-Key不一致 → 403 FORBIDDEN
    @Test
    void updateStatusMisMatchHostKey() throws Exception{
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        // room.getHostKey()とは異なるホストキー
        UUID hostKey = UUID.randomUUID();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        
        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.PROPOSED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.ACCEPTED);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("ホストキーが無効です"));
    }

    // 異常系3: itemIdが実在しない → 404 ITEM_NOT_FOUND
    @Test
    void updateStatusItemNotFound() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        // 偽のitemIdを生成
        UUID itemId = UUID.randomUUID();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.ACCEPTED);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("アイテムが見つかりません"));
    }

    // 異常系4: itemIdが別ルームのアイテム → 404 ITEM_NOT_FOUND
    // roomIdはroom2.getId()で取得、itemIdはroom1のitem.getId()で取得する
    @Test
    void updateStatusItemNotFoundAnotherRoom() throws Exception {
        Room room1 = new Room("テストルーム1");
        roomRepository.save(room1);

        Room room2 = new Room("テストルーム2");
        roomRepository.save(room2);
        UUID room2Id = room2.getId();

        // room2のホストキーを取得
        UUID hostKey = room2.getHostKey();

        // room1の参加者を作成
        Participant participant = new Participant(room1, "テスト参加者");
        participantRepository.save(participant);

        // room1でアイテムを作成
        Item item = new Item(room1, participant, "テストアイテム", 100, 1, null);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.ACCEPTED);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", room2Id, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("アイテムが見つかりません"));
    }

    // 異常系5: 不正なstatus文字列 → 400
    @Test
    void updateStatusInvalidStatusValue() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        itemRepository.save(item);
        UUID itemId = item.getId();

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"FOO\"}")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
        .andExpect(jsonPath("$.fields.status").value("不正なstatusです"));
    }

    // 異常系6: statusが未指定（null） → 400
    @Test
    void updateStatusNullStatus() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey =room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemStatusRequest request = new UpdateItemStatusRequest(null);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.status").value("statusを正しく入力してください"));
    }
}