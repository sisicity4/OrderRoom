package com.github.karuhito.orderroombackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
public class ItemControllerDeleteTest {
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

    @Test // 1.提案者本人が削除 -> 204 DBから消えていることを確認
    void deleteItemParticipant() throws Exception {
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
        
        mockMvc.perform(
            delete("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
        )
        .andExpect(status().isNoContent());

        Optional<Item> result = (itemRepository.findById(itemId));
        assertTrue(result.isEmpty(), "正常系1 : アイテムが削除されていません");
    }
    
    @Test // 2. ホストが削除 204
    void deleteItemHost() throws Exception {
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
        
        mockMvc.perform(
            delete("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
        )
        .andExpect(status().isNoContent());

        Optional<Item> result = (itemRepository.findById(itemId));
        assertTrue(result.isEmpty(), "正常系2 : アイテムが削除されていません");
    }

    /* 異常系テスト
     * 1. 他の参加者が削除 403 DBに残っている
     * 2. 存在しないitemId 404 ItemNotFound
     * 3. 両ヘッダとも無し
     */

    @Test // 1. 他の参加者が削除 403 DBに残っている
    void deleteItemAnotherParticipant() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Participant anotherParticipant = new Participant(room, "他の参加者");
        participantRepository.save(anotherParticipant);
        UUID token = anotherParticipant.getToken();

        Item item = new Item(room, participant, "テストアイテム", 100, 1, "テスト");
        item.setStatus(ItemStatus.PROPOSED);
        item.setPurchased(false);
        itemRepository.save(item);
        UUID itemId = item.getId();

        mockMvc.perform(
            delete("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("アイテムを操作する権限がありません"));

        Optional<Item> result = (itemRepository.findById(itemId));
        assertFalse(result.isEmpty(), "異常系1 : アイテムが削除されています");
    }
    @Test // 2. 存在しないitemId 404 ItemNotFound
    void deleteItemNotFound() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        

        Participant participant = new Participant(room, "テスト参加者");
        participantRepository.save(participant);

        Participant anotherParticipant = new Participant(room, "他の参加者");
        participantRepository.save(anotherParticipant);
        UUID token = anotherParticipant.getToken();

        UUID itemId = UUID.randomUUID();

        mockMvc.perform(
            delete("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
            .header("X-Participant-Token", token.toString())
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("アイテムが見つかりません"));
    }

    @Test // 3. 両ヘッダとも無し
    void deleteItemMissingHeader() throws Exception {
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

        mockMvc.perform(
            delete("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("認証情報がありません"));

        Optional<Item> result = (itemRepository.findById(itemId));
        assertFalse(result.isEmpty(), "異常系3 : アイテムが削除されています");
    }
}