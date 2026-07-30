package com.github.karuhito.orderroombackend.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class ItemControllerGetTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ItemRepository itemRepository;


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
}