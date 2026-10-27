package com.github.karuhito.orderroombackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;

/* 精算の取得（GET）と再計算（POST）のAPIのテスト */
@SpringBootTest
@AutoConfigureMockMvc
class SettlementControllerTest {
    private static final String SETTLEMENT_PATH = "/api/rooms/{roomId}/settlement";
    private static final String RECALCULATE_PATH = "/api/rooms/{roomId}/settlement/recalculate";

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired ItemRepository items;
    @Autowired SettlementRepository settlements;

    private Room room;
    private Participant a;
    private Participant b;

    /* 2人のルームで、Aが2000円を立て替えた状態（負担額は1000円ずつ。BからAへ1000円） */
    @BeforeEach
    void setup() {
        room = rooms.save(new Room("精算API"));
        a = participants.save(new Participant(room, "A"));
        b = participants.save(new Participant(room, "B"));
        Item item = new Item(room, a, "肉", 2000, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        item.setActualPrice(2000);
        item.setPaidByParticipant(a);
        items.save(item);
    }

    // ---- 正常系 ----

    @Test
    void recalculateWithHostKeyReturnsResultAndSavesTransfers() throws Exception {
        mvc.perform(post(RECALCULATE_PATH, room.getId())
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualTotalPrice").value(2000))
            .andExpect(jsonPath("$.incompletePurchaseItems").isEmpty())
            .andExpect(jsonPath("$.members.length()").value(2))
            .andExpect(jsonPath("$.transfers.length()").value(1))
            .andExpect(jsonPath("$.transfers[0].id").isNotEmpty())
            .andExpect(jsonPath("$.transfers[0].fromParticipantId").value(b.getId().toString()))
            .andExpect(jsonPath("$.transfers[0].toParticipantId").value(a.getId().toString()))
            .andExpect(jsonPath("$.transfers[0].amount").value(1000))
            .andExpect(jsonPath("$.transfers[0].settledAt").value(nullValue()))
            .andExpect(jsonPath("$.locked").value(false));

        assertThat(settlements.findByRoomId(room.getId())).hasSize(1);
    }

    @Test
    void getWithParticipantTokenReturnsOk() throws Exception {
        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Participant-Token", a.getToken().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualTotalPrice").value(2000))
            .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test
    void getWithHostKeyReturnsOk() throws Exception {
        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualTotalPrice").value(2000))
            .andExpect(jsonPath("$.members.length()").value(2));
    }

    // ---- 認証（再計算はホスト専用、取得は参加者 token かホストキー） ----

    @Test
    void recalculateWithoutHostKeyIsForbidden() throws Exception {
        mvc.perform(post(RECALCULATE_PATH, room.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void recalculateWithParticipantTokenOnlyIsForbidden() throws Exception {
        mvc.perform(post(RECALCULATE_PATH, room.getId())
                .header("X-Participant-Token", a.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        assertThat(settlements.findByRoomId(room.getId())).isEmpty();
    }

    @Test
    void getWithoutAuthenticationIsForbidden() throws Exception {
        mvc.perform(get(SETTLEMENT_PATH, room.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void getWithUnknownParticipantTokenIsForbidden() throws Exception {
        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Participant-Token", UUID.randomUUID().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
