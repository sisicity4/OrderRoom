package com.github.karuhito.orderroombackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;

/* 精算済みのリセット（POST .../settlement/reset）のAPIのテスト */
@SpringBootTest
@AutoConfigureMockMvc
class SettlementControllerResetTest {
    private static final String RESET_PATH = "/api/rooms/{roomId}/settlement/reset";
    private static final Instant SETTLED_AT = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired ItemRepository items;
    @Autowired SettlementRepository settlements;

    private Room room;
    private Participant a;
    private Settlement settled;

    /* Aが3000円を立て替え、B→A と C→A の送金案があるルーム。B→A だけ精算済み */
    @BeforeEach
    void setup() {
        room = rooms.save(new Room("リセットAPI"));
        a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Participant c = participants.save(new Participant(room, "C"));
        Item item = new Item(room, a, "肉", 3000, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        item.setActualPrice(3000);
        item.setPaidByParticipant(a);
        items.save(item);
        Settlement toBeSettled = new Settlement(room, b, a, 1000);
        toBeSettled.markSettled(SETTLED_AT);
        settled = settlements.save(toBeSettled);
        settlements.save(new Settlement(room, c, a, 1000));
    }

    private List<Settlement> storedSettlements(Room target) {
        return settlements.findByRoomId(target.getId());
    }

    @Test
    void hostKeyResetsAllSettledAtAndKeepsTransfers() throws Exception {
        mvc.perform(post(RESET_PATH, room.getId())
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locked").value(false))
            .andExpect(jsonPath("$.transfers.length()").value(2))
            .andExpect(jsonPath("$.transfers[0].settledAt").value(nullValue()))
            .andExpect(jsonPath("$.transfers[1].settledAt").value(nullValue()))
            .andExpect(jsonPath("$.members.length()").value(3));

        assertThat(storedSettlements(room)).hasSize(2)
            .allSatisfy(s -> assertThat(s.getSettledAt()).isNull());
    }

    @Test
    void resetWithoutSettledReturnsOk() throws Exception {
        settled.markUnsettled();
        settlements.save(settled);

        mvc.perform(post(RESET_PATH, room.getId())
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locked").value(false))
            .andExpect(jsonPath("$.transfers.length()").value(2));
    }

    @Test
    void nonHostCredentialsAreForbiddenAndKeepSettled() throws Exception {
        Room other = rooms.save(new Room("別ルーム"));

        // X-Host-Key なし
        mvc.perform(post(RESET_PATH, room.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        // 参加者 token のみ（支払側本人でもリセットはホスト専用）
        mvc.perform(post(RESET_PATH, room.getId())
                .header("X-Participant-Token", a.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        // 不一致・別ルームのキー・UUIDでない値
        for (String hostKey : List.of(UUID.randomUUID().toString(), other.getHostKey().toString(), "not-a-uuid")) {
            mvc.perform(post(RESET_PATH, room.getId()).header("X-Host-Key", hostKey))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }

        assertThat(settlements.findById(settled.getId()).orElseThrow().getSettledAt()).isEqualTo(SETTLED_AT);
    }

    @Test
    void unknownRoomIsNotFound() throws Exception {
        mvc.perform(post(RESET_PATH, UUID.randomUUID())
                .header("X-Host-Key", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
    }

    @Test
    void roomIdThatIsNotUuidIsTypeMismatch() throws Exception {
        mvc.perform(post(RESET_PATH, "not-a-uuid")
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
            .andExpect(jsonPath("$.fields.roomId").value("不正な値: not-a-uuid"));
    }
}
