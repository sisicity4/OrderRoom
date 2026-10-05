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

import com.github.karuhito.orderroombackend.dto.UpdateItemPurchaseDetailRequest;
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
public class ItemControllerUpdatePurchaseDetailTest {
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
    
    // 正常系: acceptedかつpurchasedの商品に、実購入金額と購入者を登録
    @Test 
    void updateItemPurchaseDetailTest() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テストユーザー1");
        participantRepository.save(participant);

        Participant paidParticipant = new Participant(room, "テストユーザー2");
        participantRepository.save(paidParticipant);
        UUID paidParticipantId = paidParticipant.getId();
        

        Item item = new Item(room, participant, "コーラ", 200, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(150, paidParticipantId);
        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("コーラ"))
        .andExpect(jsonPath("$.actualPrice").value(150))
        .andExpect(jsonPath("$.paidByParticipantId").value(paidParticipant.getId().toString()))
        .andExpect(jsonPath("$.paidByParticipantName").value(paidParticipant.getName()));
    }

    // 異常系: 認証(X-Host-Key)なし
    @Test
    void updatePurchaseDetailMissingHostKey() throws Exception{
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        Participant participant = new Participant(room, "テストユーザー1");
        participantRepository.save(participant);

        Participant paidParticipant = new Participant(room, "テストユーザー2");
        participantRepository.save(paidParticipant);
        UUID paidParticipantId = paidParticipant.getId();

        Item item = new Item(room, participant, "コーラ", 200, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(150, paidParticipantId);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("ホストキーが無効です"));
    }
}