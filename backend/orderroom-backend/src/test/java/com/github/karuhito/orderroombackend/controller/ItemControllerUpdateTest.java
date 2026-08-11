package com.github.karuhito.orderroombackend.controller;

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

import com.github.karuhito.orderroombackend.dto.UpdateItemRequest;
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
public class ItemControllerUpdateTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private  RoomRepository roomRepository;
    
    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ItemRepository itemRepository;

    // 正常系テスト
    @Test // 1. 提案者本人が編集(token) 
    void updateItemParticipant() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.participantId").value(participant.getId().toString()))
        .andExpect(jsonPath("$.name").value("ハンバーガー"))
        .andExpect(jsonPath("$.price").value(500))
        .andExpect(jsonPath("$.quantity").value(10))
        .andExpect(jsonPath("$.memo").value("ダブルチーズバーガー"))
        .andExpect(jsonPath("$.status").value("proposed"))
        .andExpect(jsonPath("$.purchased").value(false))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists())
        .andExpect(jsonPath("$.participantName").value("テスト参加者"));
    }

    @Test // 2. ホストが編集(hostKey)
    void updateItemHost() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.participantId").value(participant.getId().toString()))
        .andExpect(jsonPath("$.name").value("ハンバーガー"))
        .andExpect(jsonPath("$.price").value(500))
        .andExpect(jsonPath("$.quantity").value(10))
        .andExpect(jsonPath("$.memo").value("ダブルチーズバーガー"))
        .andExpect(jsonPath("$.status").value("proposed"))
        .andExpect(jsonPath("$.purchased").value(false))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists())
        .andExpect(jsonPath("$.participantName").value("テスト参加者"));
    }

    @Test // 3. 参加者がacceptedアイテムを編集
    void updateAcceptedItemParticipant() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        // updateが行われるとstatusがproposedに、purchasedがfalseに切り替わることを確認する
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("ハンバーガー"))
        .andExpect(jsonPath("$.status").value("proposed"))
        .andExpect(jsonPath("$.purchased").value(false));
    }

    @Test // 4. ホストがacceptedアイテムを編集
    void updateAcceptedItemHost() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        // ホストの編集ではstatusとpurchasedが維持されることを確認する
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("ハンバーガー"))
        .andExpect(jsonPath("$.status").value("accepted"))
        .andExpect(jsonPath("$.purchased").value(true));
    }

    // 異常系テスト
    @Test // 1. 他の参加者が編集 -> 403 FORBIDDEN
    void updateItemOtherParticipant() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        Participant otherParticipant = new Participant(room, "他の参加者");
        participantRepository.save(otherParticipant);
        UUID token = otherParticipant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("アイテムを操作する権限がありません"));
    }

    @Test // 2. 両ヘッダともなし -> 403 認証情報がありません
    void updateItemMissingHeader() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("認証情報がありません"));
    }

    @Test // 3. hostKeyにroomIdを送る -> 403
    void updateItemMismatchHostKey() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Host-Key", roomId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("ホストキーが無効です"));
    }

    @Test // 4. 別ルームの参加者token -> 403
    void updateItemOtherRoomToken() throws Exception {
        // 本ルームと参加者
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        // 別ルームと参加者
        Room anotherRoom = new Room("別ルーム");
        roomRepository.save(anotherRoom);

        Participant anotherParticipant = new Participant(anotherRoom, "別ルーム参加者");
        participantRepository.save(anotherParticipant);
        UUID token = anotherParticipant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("トークンが無効です"));
    }

    @Test // 5. 存在しないitemId -> 404
    void updateItemNotfound() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();
        
        UUID itemId = UUID.randomUUID();

        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("アイテムが見つかりません"));
    }

    @Test // 6. 別ルームのitemId -> 404
    void updateItemAnotherRoom() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Room anotherRoom = new Room("別ルーム");
        roomRepository.save(anotherRoom);
        
        Participant participant = new Participant(anotherRoom, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(anotherRoom, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("アイテムが見つかりません"));
    }

    // 7.バリデーション違反(name 空 | priceマイナス | quantity 0 | memo 256 文字) -> 400 VALID_ERROR
    @Test // 7.1.1 nameが空の場合
    void updateItemNotName() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("", 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("アイテム名を正しく入力してください"));
    }
    @Test // 7.1.2 nameが101文字以上の場合
    void updateItemInvalidName() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("あ".repeat(101), 500, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.name").value("アイテム名は100字以内で入力してください"));
    }

    @Test // 7.2 priceがマイナスの場合
    void updateItemInvalidPrice() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", -1, 10, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.price").value("値段をマイナスに設定することはできません"));
    }
    
    @Test // 7.3 quantityが0以下の場合
    void updateItemInvalidQuantity() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 0, "ダブルチーズバーガー");

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.quantity").value("個数を0以下に設定することはできません"));
    }

    @Test // 7.4 memoが256文字以上の場合
    void updateItemInvalidMemo() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID token = participant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
        
        UpdateItemRequest request = new UpdateItemRequest("ハンバーガー", 500, 10, "あ".repeat(256));

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.memo").value("メモは255字以内で入力してください"));
    }
}
