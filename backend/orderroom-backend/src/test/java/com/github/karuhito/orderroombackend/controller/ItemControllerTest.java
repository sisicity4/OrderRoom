package com.github.karuhito.orderroombackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
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
public class ItemControllerTest {
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
     * CreateItemのテスト
     */
    @Test // 正しい場合
    void createItem() throws Exception {
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
    void createItemNotFoundRoom() throws Exception {
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
    
    @Test // 2. roomIdは実在するがparticipantIdが実在しない
    void createItemInValidParticipantId() throws Exception {
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
    void createItemNotFoundParticipantId() throws Exception {
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

    /**
     * ルームのアイテム一覧の取得のテスト
     */

    @Test // 正常系
    void getItems() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();

        Item item1 = new Item(room, participant, "コーラ", 200, 1, "1.5リットル");
        itemRepository.save(item1);
        
        Item item2 = new Item(room, participant, "ポテチ", 150, 2, null);
        itemRepository.save(item2);
        mockMvc.perform(
            get("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].roomId").value(roomId.toString()))
        .andExpect(jsonPath("$[0].participantId").value(participantId.toString()))
        .andExpect(jsonPath("$[0].participantName").value(participant.getName()))
        .andExpect(jsonPath("$[0].name").value("コーラ"))
        .andExpect(jsonPath("$[0].price").value(200))
        .andExpect(jsonPath("$[0].quantity").value(1))
        .andExpect(jsonPath("$[0].memo").value("1.5リットル"))
        .andExpect(jsonPath("$[0].status").value("PROPOSED"))
        .andExpect(jsonPath("$[0].purchased").value(false))
        .andExpect(jsonPath("$[0].createdAt").exists())
        .andExpect(jsonPath("$[0].updatedAt").exists())

        .andExpect(jsonPath("$[1].id").exists())
        .andExpect(jsonPath("$[1].roomId").value(roomId.toString()))
        .andExpect(jsonPath("$[1].participantId").value(participantId.toString()))
        .andExpect(jsonPath("$[1].participantName").value(participant.getName()))
        .andExpect(jsonPath("$[1].name").value("ポテチ"))
        .andExpect(jsonPath("$[1].price").value(150))
        .andExpect(jsonPath("$[1].quantity").value(2))
        .andExpect(jsonPath("$[1].memo").value(nullValue()))
        .andExpect(jsonPath("$[1].status").value("PROPOSED"))
        .andExpect(jsonPath("$[1].purchased").value(false))
        .andExpect(jsonPath("$[1].createdAt").exists())
        .andExpect(jsonPath("$[1].updatedAt").exists());
    }

    @Test // 1. roomIdが実在しない
    void getItemsNotFoundRoom() throws Exception {
        UUID roomId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        
        mockMvc.perform(
            get("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
            
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("ルームが見つかりません"));
    }

    @Test // 2.roomIdの型が不一致の場合
    void getItemsInvalidRoomIdType() throws Exception {
        String roomId = "1d3afab3-8537-4275-afaa-c4268211a2xx";

        mockMvc.perform(
            get("/api/rooms/{roomId}/items", roomId)
            .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
        .andExpect(jsonPath("$.fields.roomId").value("不正な値: " + roomId));
    }


    @Test // 3.Statusが不正な値の場合
    void getItemsInvalidStatusType() throws Exception {
        UUID roomId = UUID.randomUUID();
        String invalidStatus = "FOO";

        mockMvc.perform(
            get("/api/rooms/{roomId}/items", roomId)
            .param("status", invalidStatus)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
        .andExpect(jsonPath("$.fields.status").value("不正な値: " + invalidStatus ));
    }

    @Test // 4.participantIdの型が不一致の場合
    void getItemsInvalidParticipantIdType() throws Exception {
        UUID roomId = UUID.randomUUID();
        String participantId = "1d3afab3-8537-4275-afaa-c4268211a2xx";

        mockMvc.perform(
            get("/api/rooms/{roomId}/items", roomId)
            .param("participantId", participantId)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
        .andExpect(jsonPath("$.fields.participantId").value("不正な値: " + participantId));
    }

    @Test // 5.アイテムが一件も存在しないルームにアクセスの場合
    void getItemsEmptyList() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();


        mockMvc.perform(
            get("/api/rooms/{roomId}/items", roomId)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    }

    @Test // 6.participantIdは正しいUUID形式だがそのルームに存在しない参加者IDを指定
    void getItemsNotJoinParticipantId() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item1 = new Item(room, participant, "コーラ", 200, 1, "1.5リットル");
        itemRepository.save(item1);
        
        Item item2 = new Item(room, participant, "ポテチ", 150, 2, null);
        itemRepository.save(item2);

        // ルームに存在しない偽の参加者ID
        UUID fakeParticipantId = UUID.randomUUID();

        mockMvc.perform(
            get("/api/rooms/{roomId}/items", roomId)
            .param("participantId", fakeParticipantId.toString())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    }

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