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

import com.github.karuhito.orderroombackend.dto.UpdateItemPurchasedRequest;
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
public class ItemControllerUpdatePurchasedTest {
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
    

    // 正常系: purchasedをtrue/falseに更新できる
    // 正常系1.1: false -> true
    @Test
    void updatePurchasedToTrue() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();
    
        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        UUID participantId = participant.getId();
    
        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();
    
        UpdateItemPurchasedRequest request = new UpdateItemPurchasedRequest(true);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchased", roomId, itemId)
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
        .andExpect(jsonPath("$.purchased").value(true))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
    }

    // 正常系1.2 true -> false
    @Test
    void updatePurchasedToFalse() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();
        Participant participant = new Participant(room, "テスト参加者");            participantRepository.save(participant);
        UUID participantId = participant.getId();
    
        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        itemRepository.save(item);
        UUID itemId = item.getId();
    
        UpdateItemPurchasedRequest request = new UpdateItemPurchasedRequest(false);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchased", roomId, itemId)
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

    // 異常系
    // 異常系1.StatusがACCEPTEDではない場合
    @Test
    void updatePurchasedInvalidStatus() throws Exception{
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();
    
        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
    
        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchasedRequest request = new UpdateItemPurchasedRequest(false);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchased", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("アイテムを採用済みにしている必要があります"));
    }

    // 異常系2.1 host_keyが欠如している場合
    @Test
    void updatePurchasedMissingHostKey() throws Exception{
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        
        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchasedRequest request = new UpdateItemPurchasedRequest(true);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchased", roomId, itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("ホストキーが無効です"));
    }

    // 異常系2.2 host_keyが不一致の場合
    @Test
    void updatePurchasedMisMatchHostKey() throws Exception{
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

    // 異常系3.存在しないitemIdの場合に404になる
    @Test
    void updatePurchasedItemNotFound() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();
    
        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);
        
    
        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = UUID.randomUUID();

        UpdateItemPurchasedRequest request = new UpdateItemPurchasedRequest(false);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchased", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("アイテムが見つかりません"));
    }
    // 異常系4.purchasedがnullまたは不正な型の場合に400になる

    // 異常系4.1 purchasedがnullの場合
    @Test
    void updatePurchasedMissingPurchased() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchasedRequest request = new UpdateItemPurchasedRequest(null);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchased", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.purchased").value("purchasedを正しく入力してください"));
    }

    // 異常系4.2 purchasedが不正な型の場合
    @Test
    void updatePurchasedMisMatchPurchased() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "テストアイテム", 100, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item);
        UUID itemId = item.getId();

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchased", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"purchased\": \"FOO\"}")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
        .andExpect(jsonPath("$.fields.purchased").value("不正なpurchasedです"));
    }
}