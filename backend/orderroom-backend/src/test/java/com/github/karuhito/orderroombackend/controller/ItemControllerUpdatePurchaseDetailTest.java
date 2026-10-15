package com.github.karuhito.orderroombackend.controller;


import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.github.karuhito.orderroombackend.exception.ItemNotFoundException;
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
    
    // 正常系1: acceptedかつpurchasedの商品に、実購入金額と購入者を登録
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

    @Test // 正常系2: 登録済みの商品へ別の金額・購入者で上書き
    void updateItemPurchaseDetailOverwriteTest() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テストユーザー1");
        participantRepository.save(participant);

        Participant paidByParticipant = new Participant(room, "テストユーザー2");
        participantRepository.save(paidByParticipant);
        UUID paidByParticipantId = paidByParticipant.getId();

        Item item = new Item(room, participant, "コーラ", 150, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        item.setActualPrice(200);
        item.setPaidByParticipant(participant);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.actualPrice").value(300))
        .andExpect(jsonPath("$.paidByParticipantId").value(paidByParticipantId.toString()))
        .andExpect(jsonPath("$.paidByParticipantName").value(paidByParticipant.getName()));

        Item checkItem = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        assertEquals(300, checkItem.getActualPrice());
        assertEquals(paidByParticipantId, checkItem.getPaidByParticipant().getId());
    }

    @Test // 正常系3: actualPriceとpaidByParticipantが両方nullでクリア(入力済みの商品)
    void updateItemPurchaseDetailClearTest() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テストユーザー1");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "コーラ", 150, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        item.setActualPrice(200);
        item.setPaidByParticipant(participant);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(null, null);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("コーラ"))
        .andExpect(jsonPath("$.actualPrice").value(nullValue()))
        .andExpect(jsonPath("$.paidByParticipantId").value(nullValue()))
        .andExpect(jsonPath("$.paidByParticipantName").value(nullValue()));

        Item checkItem = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        assertNull(checkItem.getActualPrice());
        assertNull(checkItem.getPaidByParticipant());
    }

    @Test // 正常系4: actualPriceとpaidByParticipantが両方nullでクリア(状態が合わない商品)
    void updateItemPurchaseDetailClearInvalidStatusNoopTest() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テストユーザー1");
        participantRepository.save(participant);

        Item item = new Item(room, participant, "コーラ", 150, 1, null);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(null, null);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("コーラ"))
        .andExpect(jsonPath("$.actualPrice").value(nullValue()))
        .andExpect(jsonPath("$.paidByParticipantId").value(nullValue()))
        .andExpect(jsonPath("$.paidByParticipantName").value(nullValue()));

        Item checkItem = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        assertNull(checkItem.getActualPrice());
        assertNull(checkItem.getPaidByParticipant());
    }

    @Test // 境界系: actualPriceを0に更新するとき
    void updateItemPurchaseDetailZeroPriceTest() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        UUID hostKey = room.getHostKey();

        Participant participant = new Participant(room, "テストユーザー1");
        participantRepository.save(participant);

        Participant paidByParticipant = new Participant(room, "テストユーザー2");
        participantRepository.save(paidByParticipant);
        UUID paidByParticipantId = paidByParticipant.getId();

        Item item = new Item(room, participant, "コーラ", 150, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        item.setActualPrice(200);
        item.setPaidByParticipant(participant);
        itemRepository.save(item);
        UUID itemId = item.getId();

        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(0, paidByParticipantId);

        mockMvc.perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
            .header("X-Host-Key", hostKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.actualPrice").value(0))
        .andExpect(jsonPath("$.paidByParticipantId").value(paidByParticipantId.toString()))
        .andExpect(jsonPath("$.paidByParticipantName").value(paidByParticipant.getName()));

        Item checkItem = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        assertEquals(0, checkItem.getActualPrice());
        assertEquals(paidByParticipantId, checkItem.getPaidByParticipant().getId());
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