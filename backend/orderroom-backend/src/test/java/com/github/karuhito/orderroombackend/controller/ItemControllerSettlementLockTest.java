package com.github.karuhito.orderroombackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;

/* 精算済みが1件以上あるとき、商品系5つの更新APIが409になることのテスト */
@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerSettlementLockTest {
    private static final String ITEM_PATH = "/api/rooms/{roomId}/items/{itemId}";
    private static final String CONFLICT_MESSAGE = "精算済みのため、変更できません";

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired ItemRepository items;
    @Autowired SettlementRepository settlements;

    private Room room;
    private Participant a;
    private Participant b;
    private Item purchased;
    private Item proposed;

    /*
     * Aが提案・購入した「肉」（採用済み・購入済み・2000円）と、Aが提案しただけの「野菜」（提案中）があるルーム。
     * B→A の送金案が精算済みになっている
     */
    @BeforeEach
    void setup() {
        room = rooms.save(new Room("精算済みの変更ロック"));
        a = participants.save(new Participant(room, "A"));
        b = participants.save(new Participant(room, "B"));
        Item meat = new Item(room, a, "肉", 2000, 1, null);
        meat.setStatus(ItemStatus.ACCEPTED);
        meat.setPurchased(true);
        meat.setActualPrice(2000);
        meat.setPaidByParticipant(a);
        purchased = items.save(meat);
        proposed = items.save(new Item(room, a, "野菜", 500, 1, null));
        saveSettled(room, b, a);
    }

    private void saveSettled(Room target, Participant from, Participant to) {
        Settlement settlement = new Settlement(target, from, to, 1000);
        settlement.markSettled(Instant.parse("2026-02-01T00:00:00Z"));
        settlements.save(settlement);
    }

    private String hostKey() {
        return room.getHostKey().toString();
    }

    private Item reload(Item item) {
        return items.findById(item.getId()).orElseThrow();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static void expectConflict(ResultActions result) throws Exception {
        result.andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value(CONFLICT_MESSAGE));
    }

    private static void expectItemNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"));
    }

    // ---- PATCH .../status ----

    @Test
    void updateStatusIsConflictAndKeepsStatus() throws Exception {
        expectConflict(mvc.perform(json(patch(ITEM_PATH + "/status", room.getId(), purchased.getId()), "{\"status\": \"rejected\"}")
            .header("X-Host-Key", hostKey())));

        Item stored = reload(purchased);
        assertThat(stored.getStatus()).isEqualTo(ItemStatus.ACCEPTED);
        assertThat(stored.isPurchased()).isTrue();
        assertThat(stored.getActualPrice()).isEqualTo(2000);
    }

    @Test
    void updateStatusOfUnknownItemIsNotFoundBeforeConflict() throws Exception {
        expectItemNotFound(mvc.perform(json(patch(ITEM_PATH + "/status", room.getId(), UUID.randomUUID()), "{\"status\": \"rejected\"}")
            .header("X-Host-Key", hostKey())));
    }

    // ---- PATCH .../purchased ----

    @Test
    void updatePurchasedIsConflictAndKeepsPurchaseDetail() throws Exception {
        expectConflict(mvc.perform(json(patch(ITEM_PATH + "/purchased", room.getId(), purchased.getId()), "{\"purchased\": false}")
            .header("X-Host-Key", hostKey())));

        Item stored = reload(purchased);
        assertThat(stored.isPurchased()).isTrue();
        assertThat(stored.getActualPrice()).isEqualTo(2000);
    }

    @Test
    void updatePurchasedIsConflictBeforeInvalidItemState() throws Exception {
        // 提案中の商品を購入済みにする（精算済みでなければ 400 INVALID_ITEM_STATE）
        expectConflict(mvc.perform(json(patch(ITEM_PATH + "/purchased", room.getId(), proposed.getId()), "{\"purchased\": true}")
            .header("X-Host-Key", hostKey())));

        assertThat(reload(proposed).isPurchased()).isFalse();
    }

    @Test
    void updatePurchasedOfUnknownItemIsNotFoundBeforeConflict() throws Exception {
        expectItemNotFound(mvc.perform(json(patch(ITEM_PATH + "/purchased", room.getId(), UUID.randomUUID()), "{\"purchased\": false}")
            .header("X-Host-Key", hostKey())));
    }

    // ---- PATCH .../{itemId}（編集） ----

    private static final String EDIT_BODY = "{\"name\": \"高い肉\", \"price\": 3000, \"quantity\": 2, \"memo\": null}";

    @Test
    void updateItemByProposerIsConflictAndKeepsItem() throws Exception {
        expectConflict(mvc.perform(json(patch(ITEM_PATH, room.getId(), purchased.getId()), EDIT_BODY)
            .header("X-Participant-Token", a.getToken().toString())));

        Item stored = reload(purchased);
        assertThat(stored.getName()).isEqualTo("肉");
        assertThat(stored.getStatus()).isEqualTo(ItemStatus.ACCEPTED);
    }

    @Test
    void updateItemByHostIsConflict() throws Exception {
        expectConflict(mvc.perform(json(patch(ITEM_PATH, room.getId(), purchased.getId()), EDIT_BODY)
            .header("X-Host-Key", hostKey())));

        assertThat(reload(purchased).getName()).isEqualTo("肉");
    }

    @Test
    void updateItemByNonProposerIsForbiddenBeforeConflict() throws Exception {
        mvc.perform(json(patch(ITEM_PATH, room.getId(), purchased.getId()), EDIT_BODY)
                .header("X-Participant-Token", b.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("アイテムを操作する権限がありません"));
    }

    @Test
    void updateUnknownItemIsNotFoundBeforeConflict() throws Exception {
        expectItemNotFound(mvc.perform(json(patch(ITEM_PATH, room.getId(), UUID.randomUUID()), EDIT_BODY)
            .header("X-Host-Key", hostKey())));
    }

    // ---- DELETE .../{itemId} ----

    @Test
    void deleteItemByProposerIsConflictAndKeepsItem() throws Exception {
        expectConflict(mvc.perform(delete(ITEM_PATH, room.getId(), purchased.getId())
            .header("X-Participant-Token", a.getToken().toString())));

        assertThat(items.findById(purchased.getId())).isPresent();
    }

    @Test
    void deleteItemByNonProposerIsForbiddenBeforeConflict() throws Exception {
        mvc.perform(delete(ITEM_PATH, room.getId(), purchased.getId())
                .header("X-Participant-Token", b.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        assertThat(items.findById(purchased.getId())).isPresent();
    }

    @Test
    void deleteUnknownItemIsNotFoundBeforeConflict() throws Exception {
        expectItemNotFound(mvc.perform(delete(ITEM_PATH, room.getId(), UUID.randomUUID())
            .header("X-Host-Key", hostKey())));
    }

    // ---- PATCH .../purchase-detail ----

    @Test
    void updatePurchaseDetailWithValueIsConflict() throws Exception {
        String body = "{\"actualPrice\": 2500, \"paidByParticipantId\": \"" + b.getId() + "\"}";
        expectConflict(mvc.perform(json(patch(ITEM_PATH + "/purchase-detail", room.getId(), purchased.getId()), body)
            .header("X-Host-Key", hostKey())));

        Item stored = reload(purchased);
        assertThat(stored.getActualPrice()).isEqualTo(2000);
        assertThat(stored.getPaidByParticipant().getId()).isEqualTo(a.getId());
    }

    @Test
    void clearingPurchaseDetailIsConflict() throws Exception {
        String body = "{\"actualPrice\": null, \"paidByParticipantId\": null}";
        expectConflict(mvc.perform(json(patch(ITEM_PATH + "/purchase-detail", room.getId(), purchased.getId()), body)
            .header("X-Host-Key", hostKey())));

        Item stored = reload(purchased);
        assertThat(stored.getActualPrice()).isEqualTo(2000);
        assertThat(stored.getPaidByParticipant()).isNotNull();
    }

    @Test
    void clearingPurchaseDetailOfItemInWrongStateIsConflict() throws Exception {
        // 提案中の商品のクリアは、精算済みでなければ何もせず200（no-op）になる。精算済みなら409（決定3）
        String body = "{\"actualPrice\": null, \"paidByParticipantId\": null}";
        expectConflict(mvc.perform(json(patch(ITEM_PATH + "/purchase-detail", room.getId(), proposed.getId()), body)
            .header("X-Host-Key", hostKey())));
    }

    @Test
    void updatePurchaseDetailIsConflictBeforeInvalidItemState() throws Exception {
        // 提案中の商品に値を入れる（精算済みでなければ 400 INVALID_ITEM_STATE）
        String body = "{\"actualPrice\": 500, \"paidByParticipantId\": \"" + a.getId() + "\"}";
        expectConflict(mvc.perform(json(patch(ITEM_PATH + "/purchase-detail", room.getId(), proposed.getId()), body)
            .header("X-Host-Key", hostKey())));

        assertThat(reload(proposed).getActualPrice()).isNull();
    }

    @Test
    void updatePurchaseDetailOfUnknownItemIsNotFoundBeforeConflict() throws Exception {
        String body = "{\"actualPrice\": null, \"paidByParticipantId\": null}";
        expectItemNotFound(mvc.perform(json(patch(ITEM_PATH + "/purchase-detail", room.getId(), UUID.randomUUID()), body)
            .header("X-Host-Key", hostKey())));
    }

    // ---- ロックが掛からない場合（ロックの取得自体は成功する） ----

    @Test
    void updatesAgainAfterReset() throws Exception {
        mvc.perform(post("/api/rooms/{roomId}/settlement/reset", room.getId())
                .header("X-Host-Key", hostKey()))
            .andExpect(status().isOk());

        mvc.perform(json(patch(ITEM_PATH + "/status", room.getId(), proposed.getId()), "{\"status\": \"rejected\"}")
                .header("X-Host-Key", hostKey()))
            .andExpect(status().isOk());
        assertThat(reload(proposed).getStatus()).isEqualTo(ItemStatus.REJECTED);
    }

    @Test
    void updatesWhenOnlyOtherRoomIsSettled() throws Exception {
        Room other = rooms.save(new Room("別ルーム"));
        Participant c = participants.save(new Participant(other, "C"));
        Participant d = participants.save(new Participant(other, "D"));
        Item othersItem = items.save(new Item(other, c, "別の肉", 1000, 1, null));
        saveSettled(other, d, c);
        // 対象ルームの精算済みは外しておく
        settlements.findByRoomId(room.getId()).forEach(s -> {
            s.markUnsettled();
            settlements.save(s);
        });

        mvc.perform(json(patch(ITEM_PATH, room.getId(), proposed.getId()), EDIT_BODY)
                .header("X-Participant-Token", a.getToken().toString()))
            .andExpect(status().isOk());
        assertThat(reload(proposed).getName()).isEqualTo("高い肉");
        assertThat(reload(othersItem).getName()).isEqualTo("別の肉");
    }
}
