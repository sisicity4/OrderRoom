package com.github.karuhito.orderroombackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.github.karuhito.orderroombackend.TestcontainersConfiguration;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;

/* 送金案の精算済み変更（PATCH .../settlements/{settlementId}/settled）のAPIのテスト */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class SettlementControllerUpdateSettledTest {
    private static final String SETTLED_PATH = "/api/rooms/{roomId}/settlements/{settlementId}/settled";
    private static final String SETTLED_TRUE = "{\"settled\": true}";
    private static final Instant SETTLED_AT = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired ItemRepository items;
    @Autowired SettlementRepository settlements;

    private Room room;
    private Participant a;
    private Participant b;
    private Participant c;
    private Settlement settlement;

    /* Aが2000円を立て替え、B→Aの未精算の送金案が1件あるルーム。Cは支払側でも受取側でもない参加者 */
    @BeforeEach
    void setup() {
        room = rooms.save(new Room("精算済み変更API"));
        a = participants.save(new Participant(room, "A"));
        b = participants.save(new Participant(room, "B"));
        c = participants.save(new Participant(room, "C"));
        Item item = new Item(room, a, "肉", 2000, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        item.setActualPrice(2000);
        item.setPaidByParticipant(a);
        items.save(item);
        settlement = settlements.save(new Settlement(room, b, a, 1000));
    }

    private MockHttpServletRequestBuilder patchSettled(Object roomId, Object settlementId, String body) {
        return patch(SETTLED_PATH, roomId, settlementId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    private Instant storedSettledAt(Settlement target) {
        return settlements.findById(target.getId()).orElseThrow().getSettledAt();
    }

    // ---- 正常系 ----

    @Test
    void payerTokenMarksSettledAndLocks() throws Exception {
        mvc.perform(patchSettled(room.getId(), settlement.getId(), SETTLED_TRUE)
                .header("X-Participant-Token", b.getToken().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locked").value(true))
            .andExpect(jsonPath("$.transfers.length()").value(1))
            .andExpect(jsonPath("$.transfers[0].id").value(settlement.getId().toString()))
            .andExpect(jsonPath("$.transfers[0].settledAt").isNotEmpty())
            .andExpect(jsonPath("$.members.length()").value(3));

        assertThat(storedSettledAt(settlement)).isNotNull();
    }

    @Test
    void hostKeyMarksSettled() throws Exception {
        mvc.perform(patchSettled(room.getId(), settlement.getId(), SETTLED_TRUE)
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locked").value(true))
            .andExpect(jsonPath("$.transfers[0].settledAt").isNotEmpty());

        assertThat(storedSettledAt(settlement)).isNotNull();
    }

    @Test
    void payerTokenCanUnsettle() throws Exception {
        settlement.markSettled(SETTLED_AT);
        settlements.save(settlement);

        mvc.perform(patchSettled(room.getId(), settlement.getId(), "{\"settled\": false}")
                .header("X-Participant-Token", b.getToken().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locked").value(false))
            .andExpect(jsonPath("$.transfers[0].settledAt").value(nullValue()));

        assertThat(storedSettledAt(settlement)).isNull();
    }

    // ---- 権限・認証（403） ----

    @Test
    void participantWhoIsNeitherPayerNorReceiverIsForbidden() throws Exception {
        mvc.perform(patchSettled(room.getId(), settlement.getId(), SETTLED_TRUE)
                .header("X-Participant-Token", c.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("送金案を操作する権限がありません"));

        assertThat(storedSettledAt(settlement)).isNull();
    }

    @Test
    void receiverTokenIsForbidden() throws Exception {
        mvc.perform(patchSettled(room.getId(), settlement.getId(), SETTLED_TRUE)
                .header("X-Participant-Token", a.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("送金案を操作する権限がありません"));

        assertThat(storedSettledAt(settlement)).isNull();
    }

    @Test
    void withoutAuthenticationIsForbidden() throws Exception {
        mvc.perform(patchSettled(room.getId(), settlement.getId(), SETTLED_TRUE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        assertThat(storedSettledAt(settlement)).isNull();
    }

    @Test
    void unknownOrOtherRoomsCredentialsAreForbidden() throws Exception {
        Room other = rooms.save(new Room("別ルーム"));
        Participant outsider = participants.save(new Participant(other, "D"));

        mvc.perform(patchSettled(room.getId(), settlement.getId(), SETTLED_TRUE)
                .header("X-Participant-Token", UUID.randomUUID().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(patchSettled(room.getId(), settlement.getId(), SETTLED_TRUE)
                .header("X-Participant-Token", outsider.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(patchSettled(room.getId(), settlement.getId(), SETTLED_TRUE)
                .header("X-Host-Key", other.getHostKey().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        assertThat(storedSettledAt(settlement)).isNull();
    }

    @Test
    void authenticationIsCheckedBeforeRequestBody() throws Exception {
        // 認証なしで本文も不正なとき、入力検証の400より認証の403が先に出る
        mvc.perform(patchSettled(room.getId(), settlement.getId(), "{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ---- 入力検証（400） ----

    @Test
    void missingOrNullSettledIsValidError() throws Exception {
        for (String body : new String[] {"{}", "{\"settled\": null}"}) {
            mvc.perform(patchSettled(room.getId(), settlement.getId(), body)
                    .header("X-Host-Key", room.getHostKey().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALID_ERROR"))
                .andExpect(jsonPath("$.fields.settled").value("settledを正しく入力してください"));
        }

        assertThat(storedSettledAt(settlement)).isNull();
    }

    @Test
    void nonBooleanSettledIsTypeMismatch() throws Exception {
        mvc.perform(patchSettled(room.getId(), settlement.getId(), "{\"settled\": \"FOO\"}")
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
            .andExpect(jsonPath("$.fields.settled").value("不正なsettledです"));

        assertThat(storedSettledAt(settlement)).isNull();
    }

    @Test
    void settlementIdThatIsNotUuidIsTypeMismatch() throws Exception {
        mvc.perform(patchSettled(room.getId(), "not-a-uuid", SETTLED_TRUE)
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
            .andExpect(jsonPath("$.fields.settlementId").value("不正な値: not-a-uuid"));
    }

    @Test
    void roomIdThatIsNotUuidIsTypeMismatch() throws Exception {
        mvc.perform(patchSettled("not-a-uuid", settlement.getId(), SETTLED_TRUE)
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
            .andExpect(jsonPath("$.fields.roomId").value("不正な値: not-a-uuid"));
    }

    // ---- 存在（404） ----

    @Test
    void unknownSettlementIsNotFound() throws Exception {
        mvc.perform(patchSettled(room.getId(), UUID.randomUUID(), SETTLED_TRUE)
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("SETTLEMENT_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("送金案が見つかりません"));
    }

    @Test
    void otherRoomsSettlementIsNotFound() throws Exception {
        Room other = rooms.save(new Room("別ルーム"));
        Participant d = participants.save(new Participant(other, "D"));
        Participant e = participants.save(new Participant(other, "E"));
        Settlement othersSettlement = settlements.save(new Settlement(other, d, e, 500));

        mvc.perform(patchSettled(room.getId(), othersSettlement.getId(), SETTLED_TRUE)
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("SETTLEMENT_NOT_FOUND"));

        assertThat(storedSettledAt(othersSettlement)).isNull();
    }

    @Test
    void unknownRoomWithHostKeyIsNotFound() throws Exception {
        mvc.perform(patchSettled(UUID.randomUUID(), settlement.getId(), SETTLED_TRUE)
                .header("X-Host-Key", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
    }
}
