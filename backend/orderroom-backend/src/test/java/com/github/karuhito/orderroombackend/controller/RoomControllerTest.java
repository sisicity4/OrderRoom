package com.github.karuhito.orderroombackend.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.github.karuhito.orderroombackend.dto.CreateRoomRequest;
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

public class RoomControllerTest {
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
    


    // CreateRoom
    @Test
    void createRoomTest() throws Exception {
        // リクエストDTOを組み立てる
        CreateRoomRequest request = new CreateRoomRequest("testTitle", null, null);

        // mockMvcでPOSTリクエストを送る
        mockMvc.perform(
            post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        // 検証
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("testTitle"))
        .andExpect(jsonPath("$.hostKey").exists());
    }

    @Test
    void createRoomValidTest() throws Exception {
        // 全てnull
        CreateRoomRequest request = new CreateRoomRequest(null, null, null);

        mockMvc.perform(
            post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.title").value("空白は許可されていません"));
    }

    // アイテム集計のテスト
    @Test
    void getItemSummary() throws Exception {
        // ルーム作成
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();
        
        // 参加者を3人作成
        Participant participant1 = new Participant(room, "テストユーザー1");
        participantRepository.save(participant1);

        Participant participant2 = new Participant(room, "テストユーザー2");
        participantRepository.save(participant2);

        Participant participant3 = new Participant(room, "テストユーザー3");
        participantRepository.save(participant3);
 
        // 同じ参加者でStatusがACCEPTEDのアイテムを2つ用意する
        Item item1 = new Item(room, participant1, "テストアイテム1", 100, 1, null);
        item1.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item1);
        // 値段を200、個数を2に設定
        Item item2 = new Item(room, participant1, "テストアイテム2", 200, 2, null);
        item2.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item2);

        // PROPOSEDのアイテムを1つ作成
        Item item3 = new Item(room, participant1, "テストアイテム3", 100, 1, null);
        itemRepository.save(item3);

        // REJECTEDのアイテムを作成
        Item item4 = new Item(room, participant1, "テストアイテム4", 100, 1, null);
        item4.setStatus(ItemStatus.REJECTED);
        itemRepository.save(item4);

        // participant2 でアイテムを作成。
        // 同じ商品名で個数を3個にする
        Item item5 = new Item(room, participant2, "テストアイテム1", 100, 3, null);
        item5.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item5);

        Item item6 = new Item(room, participant2, "テストアイテム5", 100, 1, null);
        itemRepository.save(item6);

        // participant3はREJECTEDのアイテムのみを持つ
        Item item7 = new Item(room, participant3, "テストアイテム6", 100, 3, null);
        item7.setStatus(ItemStatus.REJECTED);
        itemRepository.save(item7);

        Item item8 = new Item(room, participant3, "テストアイテム7", 100, 1, null);
        item8.setStatus(ItemStatus.REJECTED);
        itemRepository.save(item8);

        mockMvc.perform(
            get("/api/rooms/{roomId}/summary", roomId)
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPrice").value(1000))
        .andExpect(jsonPath("$.participantSummaries.length()").value(2))
        .andExpect(jsonPath("$.participantSummaries[0].id").value(participant1.getId().toString()))
        .andExpect(jsonPath("$.participantSummaries[0].name").value("テストユーザー1"))
        .andExpect(jsonPath("$.participantSummaries[0].totalPrice").value(600))
        .andExpect(jsonPath("$.participantSummaries[1].id").value(participant2.getId().toString()))
        .andExpect(jsonPath("$.participantSummaries[1].name").value("テストユーザー2"))
        .andExpect(jsonPath("$.participantSummaries[1].totalPrice").value(400))
        .andExpect(jsonPath("$.itemNameSummaries.length()").value(4))
        .andExpect(jsonPath("$.itemNameSummaries[0].name").value("テストアイテム1"))
        .andExpect(jsonPath("$.itemNameSummaries[0].totalQuantity").value(4))
        .andExpect(jsonPath("$.itemNameSummaries[1].name").value("テストアイテム2"))
        .andExpect(jsonPath("$.itemNameSummaries[1].totalQuantity").value(2))
        .andExpect(jsonPath("$.itemNameSummaries[2].name").value("テストアイテム3"))
        .andExpect(jsonPath("$.itemNameSummaries[2].totalQuantity").value(1))
        .andExpect(jsonPath("$.itemNameSummaries[3].name").value("テストアイテム5"))
        .andExpect(jsonPath("$.itemNameSummaries[3].totalQuantity").value(1));
    }
    
    @Test // roomIdが見つからない
    void getItemSummaryNotFoundRoom() throws Exception {
        UUID roomId = UUID.randomUUID();
        
        mockMvc.perform(
            get("/api/rooms/{roomId}/summary", roomId)
            .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("ルームが見つかりません"));
    }

    @Test // アイテムが存在しない場合
    void getItemSummaryEmptyList() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        mockMvc.perform(
            get("/api/rooms/{roomId}/summary", roomId)
            
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPrice").value(0))
        .andExpect(jsonPath("$.participantSummaries").isEmpty())
        .andExpect(jsonPath("$.itemNameSummaries").isEmpty());



    }
}
