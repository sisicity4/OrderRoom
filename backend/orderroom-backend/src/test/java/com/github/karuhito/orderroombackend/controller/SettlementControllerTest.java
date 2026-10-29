package com.github.karuhito.orderroombackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.github.karuhito.orderroombackend.service.SettlementMemberService;

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
    @Autowired SettlementMemberService settlementMemberService;

    private Room room;
    private Participant a;
    private Participant b;
    private Item purchasedItem;

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
        purchasedItem = items.save(item);
    }

    private static String hostKeyOf(Room target) {
        return target.getHostKey().toString();
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

    // ---- 正常系（続き） ----

    @Test
    void recalculateTwiceReplacesTransfersWithoutDuplicating() throws Exception {
        String firstId = mvc.perform(post(RECALCULATE_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        mvc.perform(post(RECALCULATE_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transfers.length()").value(1))
            .andExpect(jsonPath("$.transfers[0].amount").value(1000));

        // 増えずに置き換わる（件数は1件のまま）
        assertThat(settlements.findByRoomId(room.getId())).hasSize(1);
        assertThat(firstId).contains("\"amount\":1000");
    }

    @Test
    void getBeforeAnyRecalculationReturnsMembersButNoTransfers() throws Exception {
        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.members.length()").value(2))
            .andExpect(jsonPath("$.members[0].name").value("A"))
            .andExpect(jsonPath("$.members[0].burdenAmount").value(1000))
            .andExpect(jsonPath("$.members[0].paidAmount").value(2000))
            .andExpect(jsonPath("$.members[0].balanceAmount").value(1000))
            .andExpect(jsonPath("$.members[1].name").value("B"))
            .andExpect(jsonPath("$.members[1].burdenAmount").value(1000))
            .andExpect(jsonPath("$.members[1].paidAmount").value(0))
            .andExpect(jsonPath("$.members[1].balanceAmount").value(-1000))
            .andExpect(jsonPath("$.transfers").isEmpty())
            .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    void getListsItemsMissingPriceAsIncompleteAndExcludesThemFromTotal() throws Exception {
        Item noPrice = new Item(room, a, "金額なし", 500, 1, null);
        noPrice.setStatus(ItemStatus.ACCEPTED);
        noPrice.setPurchased(true);
        noPrice.setPaidByParticipant(a);
        Item saved = items.save(noPrice);

        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualTotalPrice").value(2000))
            .andExpect(jsonPath("$.incompletePurchaseItems.length()").value(1))
            .andExpect(jsonPath("$.incompletePurchaseItems[0].id").value(saved.getId().toString()))
            .andExpect(jsonPath("$.incompletePurchaseItems[0].name").value("金額なし"));
    }

    @Test
    void includesPurchaserOutsideSettlementMembersWithZeroBurden() throws Exception {
        // Bだけを精算対象にする。購入者のAは対象外だが members に含まれ、負担額は0
        settlementMemberService.replaceMembers(room.getId(), List.of(b.getId()));

        mvc.perform(post(RECALCULATE_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.members.length()").value(2))
            .andExpect(jsonPath("$.members[0].name").value("A"))
            .andExpect(jsonPath("$.members[0].burdenAmount").value(0))
            .andExpect(jsonPath("$.members[0].paidAmount").value(2000))
            .andExpect(jsonPath("$.members[0].balanceAmount").value(2000))
            .andExpect(jsonPath("$.members[1].name").value("B"))
            .andExpect(jsonPath("$.members[1].burdenAmount").value(2000))
            .andExpect(jsonPath("$.members[1].balanceAmount").value(-2000))
            .andExpect(jsonPath("$.transfers.length()").value(1))
            .andExpect(jsonPath("$.transfers[0].fromParticipantId").value(b.getId().toString()))
            .andExpect(jsonPath("$.transfers[0].toParticipantId").value(a.getId().toString()))
            .andExpect(jsonPath("$.transfers[0].amount").value(2000));
    }

    @Test
    void dividesBurdenByNumberOfSelectedMembersOnly() throws Exception {
        // 3人いるが、精算対象はAとBの2人。対象外のCは購入者ではないので members に出ない
        participants.save(new Participant(room, "C"));
        settlementMemberService.replaceMembers(room.getId(), List.of(a.getId(), b.getId()));

        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.members.length()").value(2))
            .andExpect(jsonPath("$.members[0].burdenAmount").value(1000))
            .andExpect(jsonPath("$.members[1].burdenAmount").value(1000));
    }

    @Test
    void recalculateWithoutItemsReturnsZeroTotalAndNoTransfers() throws Exception {
        Room emptyRoom = rooms.save(new Room("商品なし"));
        participants.save(new Participant(emptyRoom, "A"));
        participants.save(new Participant(emptyRoom, "B"));

        mvc.perform(post(RECALCULATE_PATH, emptyRoom.getId())
                .header("X-Host-Key", hostKeyOf(emptyRoom)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualTotalPrice").value(0))
            .andExpect(jsonPath("$.incompletePurchaseItems").isEmpty())
            .andExpect(jsonPath("$.members.length()").value(2))
            .andExpect(jsonPath("$.transfers").isEmpty());

        assertThat(settlements.findByRoomId(emptyRoom.getId())).isEmpty();
    }

    @Test
    void getForRoomWithoutParticipantsReturnsEmptyMembersAndTransfers() throws Exception {
        Room emptyRoom = rooms.save(new Room("参加者なし"));

        mvc.perform(get(SETTLEMENT_PATH, emptyRoom.getId())
                .header("X-Host-Key", hostKeyOf(emptyRoom)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualTotalPrice").value(0))
            .andExpect(jsonPath("$.members").isEmpty())
            .andExpect(jsonPath("$.transfers").isEmpty());
    }

    @Test
    void getAfterChangingPurchaseKeepsStoredTransfersButRefreshesMembers() throws Exception {
        mvc.perform(post(RECALCULATE_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isOk());

        // 再計算の後で実購入額を2000円から4000円に変更する
        purchasedItem.setActualPrice(4000);
        items.save(purchasedItem);

        // members はその都度計算されて最新（負担額は2000円ずつ）、transfers は保存済みのまま（1000円）
        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualTotalPrice").value(4000))
            .andExpect(jsonPath("$.members[1].burdenAmount").value(2000))
            .andExpect(jsonPath("$.transfers.length()").value(1))
            .andExpect(jsonPath("$.transfers[0].amount").value(1000));
    }

    // ---- 異常系（続き） ----

    @Test
    void recalculateForRoomWithoutParticipantsIsBadRequest() throws Exception {
        Room emptyRoom = rooms.save(new Room("参加者なし"));

        mvc.perform(post(RECALCULATE_PATH, emptyRoom.getId())
                .header("X-Host-Key", hostKeyOf(emptyRoom)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("NO_SETTLEMENT_MEMBERS"));
    }

    @Test
    void recalculateWithWrongHostKeyIsForbiddenAndSavesNothing() throws Exception {
        Room other = rooms.save(new Room("別ルーム"));
        // 一致しないキー / 別ルームのキー / UUID として不正なキー
        for (String hostKey : List.of(UUID.randomUUID().toString(), hostKeyOf(other), "not-a-uuid")) {
            mvc.perform(post(RECALCULATE_PATH, room.getId()).header("X-Host-Key", hostKey))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }

        assertThat(settlements.findByRoomId(room.getId())).isEmpty();
    }

    @Test
    void getWithOtherRoomsCredentialsIsForbidden() throws Exception {
        Room other = rooms.save(new Room("別ルーム"));
        Participant outsider = participants.save(new Participant(other, "C"));

        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Participant-Token", outsider.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(get(SETTLEMENT_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(other)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void unknownRoomIsNotFound() throws Exception {
        UUID unknownRoomId = UUID.randomUUID();
        String anyHostKey = UUID.randomUUID().toString();

        mvc.perform(post(RECALCULATE_PATH, unknownRoomId).header("X-Host-Key", anyHostKey))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
        mvc.perform(get(SETTLEMENT_PATH, unknownRoomId).header("X-Host-Key", anyHostKey))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
    }

    @Test
    void recalculateWithAnySettledIsConflictAndKeepsTransfers() throws Exception {
        Settlement settled = new Settlement(room, b, a, 1000);
        settled.markSettled(Instant.parse("2026-02-01T00:00:00Z"));
        settlements.save(settled);

        mvc.perform(post(RECALCULATE_PATH, room.getId())
                .header("X-Host-Key", hostKeyOf(room)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value("精算済みのため、変更できません"))
            .andExpect(jsonPath("$.fields").value(nullValue()));

        assertThat(settlements.findByRoomId(room.getId()))
            .extracting(Settlement::getId).containsExactly(settled.getId());
    }

    @Test
    void roomIdThatIsNotUuidIsTypeMismatch() throws Exception {
        String anyHostKey = UUID.randomUUID().toString();

        mvc.perform(get(SETTLEMENT_PATH, "not-a-uuid").header("X-Host-Key", anyHostKey))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
            .andExpect(jsonPath("$.fields.roomId").value("不正な値: not-a-uuid"));
        mvc.perform(post(RECALCULATE_PATH, "not-a-uuid").header("X-Host-Key", anyHostKey))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
            .andExpect(jsonPath("$.fields.roomId").value("不正な値: not-a-uuid"));
    }
}
