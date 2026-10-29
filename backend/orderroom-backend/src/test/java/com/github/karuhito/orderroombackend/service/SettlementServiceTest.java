package com.github.karuhito.orderroombackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.github.karuhito.orderroombackend.dto.SettlementResponse;
import com.github.karuhito.orderroombackend.dto.SettlementResponse.IncompletePurchaseItem;
import com.github.karuhito.orderroombackend.dto.SettlementResponse.MemberSummary;
import com.github.karuhito.orderroombackend.dto.SettlementResponse.TransferSummary;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.exception.NoSettlementMembersException;
import com.github.karuhito.orderroombackend.exception.NotSettlementPayerException;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.exception.SettlementNotFoundException;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;
import com.github.karuhito.orderroombackend.resolver.Operator;
import com.github.karuhito.orderroombackend.resolver.OperatorType;

import jakarta.persistence.EntityManager;

/* 精算計算のServiceを、実際のDBで確認する（テスト後にロールバックするので、DBにデータは残らない） */
@SpringBootTest
@Transactional
class SettlementServiceTest {
    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant SETTLED_AT = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired SettlementService service;
    @Autowired SettlementMemberService settlementMemberService;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired ItemRepository items;
    @Autowired SettlementRepository settlements;
    @Autowired EntityManager entityManager;

    // ---- テスト用の部品 ----

    /* 参加登録の早さ（BASEからの秒数）を指定して参加者を作る。指定しないと並び順が不安定になるため */
    private Participant participant(Room room, String name, int seconds) {
        Participant participant = new Participant(room, name);
        ReflectionTestUtils.setField(participant, "createdAt", BASE.plusSeconds(seconds));
        return participants.save(participant);
    }

    /* 登録日時に加えてIDも固定した参加者。登録日時が同じ場合の並びを確かめるために使う */
    private Participant participantWithId(Room room, String name, int seconds, int idNumber) {
        Participant participant = new Participant(room, name);
        ReflectionTestUtils.setField(participant, "id", new UUID(0L, idNumber));
        ReflectionTestUtils.setField(participant, "createdAt", BASE.plusSeconds(seconds));
        return participants.save(participant);
    }

    /* 採用済み・購入済みで、実購入額と購入者が入力済みの商品（精算の計算対象） */
    private Item purchasedItem(Room room, Participant proposer, String name, int actualPrice, Participant payer) {
        Item item = new Item(room, proposer, name, actualPrice, 1, null);
        item.setStatus(ItemStatus.ACCEPTED);
        item.setPurchased(true);
        item.setActualPrice(actualPrice);
        item.setPaidByParticipant(payer);
        return items.save(item);
    }

    /* 商品の作成日時を決めて保存する（未入力の商品の並び順の確認用） */
    private Item itemCreatedAt(Item item, int seconds) {
        ReflectionTestUtils.setField(item, "createdAt", BASE.plusSeconds(seconds));
        return items.save(item);
    }

    private Settlement settledSettlement(Room room, Participant from, Participant to, int amount) {
        Settlement settlement = new Settlement(room, from, to, amount);
        settlement.markSettled(SETTLED_AT);
        return settlements.save(settlement);
    }

    /* 精算済み変更の確認用: 支払側(B)→受取側(A)の未精算の送金案が1件と、どちらでもない参加者(C)がいるルーム */
    private record Fixture(Room room, Participant receiver, Participant payer, Participant other, Settlement settlement) {
    }

    private Fixture fixture(String roomName) {
        Room room = rooms.save(new Room(roomName));
        Participant a = participant(room, "A", 0);
        Participant b = participant(room, "B", 1);
        Participant c = participant(room, "C", 2);
        Settlement settlement = settlements.save(new Settlement(room, b, a, 1000));
        return new Fixture(room, a, b, c, settlement);
    }

    private static Operator host() {
        return new Operator(OperatorType.HOST, null);
    }

    private static Operator asParticipant(Participant participant) {
        return new Operator(OperatorType.PARTICIPANT, participant.getId());
    }

    /* 1次キャッシュを捨てて、DBに保存されている settledAt を読み直す */
    private Instant storedSettledAt(UUID settlementId) {
        entityManager.flush();
        entityManager.clear();
        return settlements.findById(settlementId).orElseThrow().getSettledAt();
    }

    private static List<Object> memberRow(MemberSummary m) {
        return List.of(m.name(), m.burdenAmount(), m.paidAmount(), m.balanceAmount());
    }

    // ---- recalculate: 計算と保存 ----

    @Test
    void recalculateReturnsCalculationAndSavesTransfers() {
        Room room = rooms.save(new Room("再計算"));
        Participant a = participant(room, "A", 0);
        Participant b = participant(room, "B", 1);
        Participant c = participant(room, "C", 2);
        purchasedItem(room, a, "肉", 9000, a);

        SettlementResponse response = service.recalculate(room.getId());

        assertThat(response.actualTotalPrice()).isEqualTo(9000);
        assertThat(response.locked()).isFalse();
        assertThat(response.incompletePurchaseItems()).isEmpty();
        assertThat(response.members()).extracting(SettlementServiceTest::memberRow).containsExactly(
            List.of("A", 3000, 9000, 6000),
            List.of("B", 3000, 0, -3000),
            List.of("C", 3000, 0, -3000));
        assertThat(response.transfers())
            .extracting(TransferSummary::fromParticipantId, TransferSummary::toParticipantId, TransferSummary::amount)
            .containsExactly(tuple(b.getId(), a.getId(), 3000), tuple(c.getId(), a.getId(), 3000));
        assertThat(response.transfers()).allSatisfy(t -> {
            assertThat(t.id()).isNotNull();
            assertThat(t.settledAt()).isNull();
        });
        // 返した送金案と同じものが、未精算として保存されている
        assertThat(settlements.findByRoomIdAndSettledAtIsNull(room.getId()))
            .extracting(Settlement::getId)
            .containsExactlyInAnyOrderElementsOf(response.transfers().stream().map(TransferSummary::id).toList());
    }

    @Test
    void recalculateIncludesPurchaserOutsideSettlementMembersWithZeroBurden() {
        Room room = rooms.save(new Room("対象外の購入者"));
        Participant a = participant(room, "A", 0);
        Participant x = participant(room, "X", 1);
        Participant b = participant(room, "B", 2);
        settlementMemberService.replaceMembers(room.getId(), List.of(a.getId(), b.getId()));
        purchasedItem(room, x, "ケーキ", 10001, x);

        SettlementResponse response = service.recalculate(room.getId());

        // Xは対象外だが購入者なので載る。余りの1円は対象者のAとBの中だけで配る
        assertThat(response.members()).extracting(SettlementServiceTest::memberRow).containsExactly(
            List.of("A", 5001, 0, -5001),
            List.of("X", 0, 10001, 10001),
            List.of("B", 5000, 0, -5000));
        assertThat(response.transfers())
            .extracting(TransferSummary::fromParticipantId, TransferSummary::toParticipantId, TransferSummary::amount)
            .containsExactly(tuple(a.getId(), x.getId(), 5001), tuple(b.getId(), x.getId(), 5000));
    }

    @Test
    void recalculateReplacesOnlyUnsettledTransfersOfTheRoom() {
        Room room = rooms.save(new Room("置き換え"));
        Participant a = participant(room, "A", 0);
        Participant b = participant(room, "B", 1);
        purchasedItem(room, a, "肉", 4000, a);
        Settlement oldUnsettled = settlements.save(new Settlement(room, b, a, 111));
        Settlement settled = settledSettlement(room, a, b, 222);
        Room other = rooms.save(new Room("別ルーム"));
        Participant c = participant(other, "C", 0);
        Participant d = participant(other, "D", 1);
        Settlement otherUnsettled = settlements.save(new Settlement(other, c, d, 333));

        SettlementResponse response = service.recalculate(room.getId());

        List<UUID> remainingIds = settlements.findByRoomId(room.getId()).stream().map(Settlement::getId).toList();
        // 古い未精算は消え、精算済みは残り、新しい送金案(B→A 2000)が1件ある
        assertThat(remainingIds).doesNotContain(oldUnsettled.getId()).contains(settled.getId()).hasSize(2);
        assertThat(settlements.findByRoomIdAndSettledAtIsNull(room.getId()))
            .extracting(Settlement::getAmount).containsExactly(2000);
        assertThat(response.transfers())
            .extracting(TransferSummary::amount).contains(2000);
        // 別ルームには触れない
        assertThat(settlements.findByRoomId(other.getId()))
            .extracting(Settlement::getId).containsExactly(otherUnsettled.getId());
    }

    @Test
    void recalculateTwiceDoesNotDuplicateTransfers() {
        Room room = rooms.save(new Room("二回再計算"));
        Participant a = participant(room, "A", 0);
        participant(room, "B", 1);
        purchasedItem(room, a, "肉", 4000, a);

        service.recalculate(room.getId());
        SettlementResponse second = service.recalculate(room.getId());

        assertThat(second.transfers()).hasSize(1);
        assertThat(settlements.findByRoomId(room.getId())).hasSize(1);
    }

    @Test
    void recalculateSavesNothingWhenEveryoneIsEven() {
        Room room = rooms.save(new Room("差額なし"));
        Participant a = participant(room, "A", 0);
        Participant b = participant(room, "B", 1);
        purchasedItem(room, a, "肉", 1000, a);
        purchasedItem(room, b, "野菜", 1000, b);

        SettlementResponse response = service.recalculate(room.getId());

        assertThat(response.transfers()).isEmpty();
        assertThat(settlements.findByRoomId(room.getId())).isEmpty();
    }

    // ---- 計算対象の商品の選別 ----

    @Test
    void listsAcceptedPurchasedItemsMissingPriceOrPayerAsIncomplete() {
        Room room = rooms.save(new Room("未入力"));
        Participant a = participant(room, "A", 0);
        Participant b = participant(room, "B", 1);
        purchasedItem(room, a, "入力済み", 2000, a);

        // 実購入額が未入力(後から作ったが、作成日時は早い)
        Item noPrice = new Item(room, a, "金額なし", 500, 1, null);
        noPrice.setStatus(ItemStatus.ACCEPTED);
        noPrice.setPurchased(true);
        noPrice.setPaidByParticipant(a);
        // 購入者が未入力
        Item noPayer = new Item(room, b, "購入者なし", 500, 1, null);
        noPayer.setStatus(ItemStatus.ACCEPTED);
        noPayer.setPurchased(true);
        noPayer.setActualPrice(500);
        // 保存の順を逆にして、作成日時の順に並ぶことを確認する
        Item savedNoPayer = itemCreatedAt(noPayer, 20);
        Item savedNoPrice = itemCreatedAt(noPrice, 10);

        // 計算対象にも未入力にも入らない商品(購入済みでない / 採用されていない / 見送り)
        Item notPurchased = new Item(room, a, "未購入", 500, 1, null);
        notPurchased.setStatus(ItemStatus.ACCEPTED);
        items.save(notPurchased);
        Item proposed = new Item(room, a, "提案中", 500, 1, null);
        proposed.setActualPrice(500);
        proposed.setPaidByParticipant(a);
        items.save(proposed);
        Item rejected = new Item(room, a, "見送り", 500, 1, null);
        rejected.setStatus(ItemStatus.REJECTED);
        items.save(rejected);

        SettlementResponse response = service.recalculate(room.getId());

        assertThat(response.incompletePurchaseItems())
            .extracting(IncompletePurchaseItem::id, IncompletePurchaseItem::name)
            .containsExactly(
                tuple(savedNoPrice.getId(), "金額なし"),
                tuple(savedNoPayer.getId(), "購入者なし"));
        // 合計には入力済みの商品だけが入る
        assertThat(response.actualTotalPrice()).isEqualTo(2000);
    }

    @Test
    void ignoresItemsOfOtherRooms() {
        Room room = rooms.save(new Room("対象ルーム"));
        Participant a = participant(room, "A", 0);
        participant(room, "B", 1);
        purchasedItem(room, a, "肉", 2000, a);
        Room other = rooms.save(new Room("別ルーム"));
        Participant c = participant(other, "C", 0);
        purchasedItem(other, c, "別の肉", 9999, c);

        SettlementResponse response = service.recalculate(room.getId());

        assertThat(response.actualTotalPrice()).isEqualTo(2000);
        assertThat(response.members()).extracting(MemberSummary::name).containsExactly("A", "B");
    }

    // ---- recalculate: エラー ----

    @Test
    void recalculateThrowsNoSettlementMembersAndDoesNotTouchOtherRooms() {
        // 参加者がいない(=対象者が0人)ルーム
        Room room = rooms.save(new Room("対象者なし"));
        Room other = rooms.save(new Room("別ルーム"));
        Participant c = participant(other, "C", 0);
        Participant d = participant(other, "D", 1);
        Settlement otherUnsettled = settlements.save(new Settlement(other, c, d, 100));

        assertThatThrownBy(() -> service.recalculate(room.getId()))
            .isInstanceOf(NoSettlementMembersException.class);
        assertThat(settlements.findByRoomId(other.getId()))
            .extracting(Settlement::getId).containsExactly(otherUnsettled.getId());
    }

    @Test
    void recalculateThrowsRoomNotFoundForUnknownRoom() {
        assertThatThrownBy(() -> service.recalculate(UUID.randomUUID()))
            .isInstanceOf(RoomNotFoundException.class);
    }

    // ---- getSettlement ----

    @Test
    void getSettlementDoesNotRecalculateAndReturnsStoredTransfers() {
        Room room = rooms.save(new Room("取得"));
        Participant a = participant(room, "A", 0);
        Participant b = participant(room, "B", 1);
        participant(room, "C", 2);
        purchasedItem(room, a, "肉", 9000, a);
        // 現在のデータから計算した結果とは違う送金案を、わざと保存しておく
        Settlement stored = settlements.save(new Settlement(room, b, a, 1234));

        SettlementResponse response = service.getSettlement(room.getId());

        // members はその都度計算、transfers は保存済みをそのまま返す
        assertThat(response.members()).extracting(SettlementServiceTest::memberRow).containsExactly(
            List.of("A", 3000, 9000, 6000),
            List.of("B", 3000, 0, -3000),
            List.of("C", 3000, 0, -3000));
        assertThat(response.transfers())
            .extracting(TransferSummary::id, TransferSummary::amount)
            .containsExactly(tuple(stored.getId(), 1234));
        assertThat(settlements.findByRoomId(room.getId()))
            .extracting(Settlement::getId).containsExactly(stored.getId());
        assertThat(response.locked()).isFalse();
    }

    @Test
    void getSettlementOrdersTransfersByPayerThenReceiverRegistration() {
        Room room = rooms.save(new Room("並び順"));
        Participant a = participant(room, "A", 0);
        Participant b = participant(room, "B", 1);
        Participant c = participant(room, "C", 2);
        Participant d = participant(room, "D", 3);
        // 保存の順を、期待する並びとわざと変えておく
        settlements.save(new Settlement(room, d, b, 1500));
        settlements.save(new Settlement(room, c, a, 2500));
        settlements.save(new Settlement(room, d, a, 1000));

        SettlementResponse response = service.getSettlement(room.getId());

        assertThat(response.transfers())
            .extracting(TransferSummary::fromParticipantId, TransferSummary::toParticipantId, TransferSummary::amount)
            .containsExactly(
                tuple(c.getId(), a.getId(), 2500),
                tuple(d.getId(), a.getId(), 1000),
                tuple(d.getId(), b.getId(), 1500));
    }

    @Test
    void getSettlementOrdersTransfersByParticipantIdWhenCreatedAtIsSame() {
        Room room = rooms.save(new Room("登録日時が同じ"));
        // 登録日時が同じで、保存の順（3→1→2）とID順（1→2→3）が食い違う参加者
        Participant third = participantWithId(room, "C", 0, 3);
        Participant first = participantWithId(room, "A", 0, 1);
        Participant second = participantWithId(room, "B", 0, 2);
        settlements.save(new Settlement(room, third, second, 300));
        settlements.save(new Settlement(room, second, first, 500));
        settlements.save(new Settlement(room, third, first, 700));

        SettlementResponse response = service.getSettlement(room.getId());

        // 計算クラスと同じく「登録日時 → participantId」の順（A, B, C）で並ぶ
        assertThat(response.transfers())
            .extracting(TransferSummary::fromParticipantId, TransferSummary::toParticipantId, TransferSummary::amount)
            .containsExactly(
                tuple(second.getId(), first.getId(), 500),
                tuple(third.getId(), first.getId(), 700),
                tuple(third.getId(), second.getId(), 300));
    }

    @Test
    void getSettlementReturnsEmptyListsWhenRoomHasNoParticipants() {
        Room room = rooms.save(new Room("参加者なし"));

        SettlementResponse response = service.getSettlement(room.getId());

        assertThat(response.actualTotalPrice()).isZero();
        assertThat(response.incompletePurchaseItems()).isEmpty();
        assertThat(response.members()).isEmpty();
        assertThat(response.transfers()).isEmpty();
        assertThat(response.locked()).isFalse();
    }

    @Test
    void getSettlementReturnsEmptyTransfersBeforeAnyRecalculation() {
        Room room = rooms.save(new Room("未計算"));
        Participant a = participant(room, "A", 0);
        participant(room, "B", 1);
        purchasedItem(room, a, "肉", 2000, a);

        SettlementResponse response = service.getSettlement(room.getId());

        // 再計算するまで送金案は保存されていないので空。membersは計算される
        assertThat(response.transfers()).isEmpty();
        assertThat(response.members()).hasSize(2);
        assertThat(response.actualTotalPrice()).isEqualTo(2000);
    }

    @Test
    void getSettlementThrowsRoomNotFoundForUnknownRoom() {
        assertThatThrownBy(() -> service.getSettlement(UUID.randomUUID()))
            .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void getSettlementReturnsActualSettledAtAndLockedWhenAnySettled() {
        Room room = rooms.save(new Room("精算済みあり"));
        Participant a = participant(room, "A", 0);
        Participant b = participant(room, "B", 1);
        Participant c = participant(room, "C", 2);
        Settlement unsettled = settlements.save(new Settlement(room, c, a, 500));
        Settlement settled = settledSettlement(room, b, a, 1000);

        SettlementResponse response = service.getSettlement(room.getId());

        assertThat(response.locked()).isTrue();
        assertThat(response.transfers())
            .extracting(TransferSummary::id, TransferSummary::settledAt)
            .containsExactly(tuple(settled.getId(), SETTLED_AT), tuple(unsettled.getId(), null));
    }

    // ---- updateSettled ----

    @Test
    void updateSettledByPayerRecordsSettledAtAndLocks() {
        Fixture f = fixture("支払側本人");
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

        SettlementResponse response = service.updateSettled(
            f.room().getId(), f.settlement().getId(), asParticipant(f.payer()), true);

        Instant after = Instant.now();
        assertThat(response.locked()).isTrue();
        assertThat(response.transfers()).singleElement()
            .satisfies(t -> assertThat(t.settledAt()).isBetween(before, after));
        assertThat(storedSettledAt(f.settlement().getId())).isBetween(before, after);
    }

    @Test
    void updateSettledByHostRecordsSettledAtForAnyTransfer() {
        Fixture f = fixture("ホスト");
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

        SettlementResponse response = service.updateSettled(
            f.room().getId(), f.settlement().getId(), host(), true);

        Instant after = Instant.now();
        assertThat(response.locked()).isTrue();
        assertThat(response.transfers()).singleElement()
            .satisfies(t -> assertThat(t.settledAt()).isBetween(before, after));
        assertThat(storedSettledAt(f.settlement().getId())).isBetween(before, after);
    }

    @Test
    void updateSettledRejectsParticipantWhoIsNeitherPayerNorHost() {
        Fixture f = fixture("支払側以外");

        assertThatThrownBy(() -> service.updateSettled(
                f.room().getId(), f.settlement().getId(), asParticipant(f.other()), true))
            .isInstanceOf(NotSettlementPayerException.class);
        assertThat(storedSettledAt(f.settlement().getId())).isNull();
    }

    @Test
    void updateSettledRejectsReceiver() {
        Fixture f = fixture("受取側");

        assertThatThrownBy(() -> service.updateSettled(
                f.room().getId(), f.settlement().getId(), asParticipant(f.receiver()), true))
            .isInstanceOf(NotSettlementPayerException.class);
        assertThat(storedSettledAt(f.settlement().getId())).isNull();
    }

    @Test
    void updateSettledTrueOnAlreadySettledKeepsOriginalSettledAt() {
        Fixture f = fixture("精算済みに再度true");
        f.settlement().markSettled(SETTLED_AT);
        settlements.save(f.settlement());

        SettlementResponse response = service.updateSettled(
            f.room().getId(), f.settlement().getId(), asParticipant(f.payer()), true);

        assertThat(response.transfers()).singleElement()
            .extracting(TransferSummary::settledAt).isEqualTo(SETTLED_AT);
        assertThat(storedSettledAt(f.settlement().getId())).isEqualTo(SETTLED_AT);
    }

    @Test
    void updateSettledFalseClearsSettledAtAndUnlocksWhenNoneSettled() {
        Fixture f = fixture("取り消し");
        f.settlement().markSettled(SETTLED_AT);
        settlements.save(f.settlement());

        SettlementResponse response = service.updateSettled(
            f.room().getId(), f.settlement().getId(), asParticipant(f.payer()), false);

        assertThat(response.locked()).isFalse();
        assertThat(response.transfers()).singleElement()
            .extracting(TransferSummary::settledAt).isNull();
        assertThat(storedSettledAt(f.settlement().getId())).isNull();
    }

    @Test
    void updateSettledFalseKeepsLockedWhenAnotherIsStillSettled() {
        Fixture f = fixture("一部だけ取り消し");
        f.settlement().markSettled(SETTLED_AT);
        settlements.save(f.settlement());
        Settlement stillSettled = settledSettlement(f.room(), f.other(), f.receiver(), 500);

        SettlementResponse response = service.updateSettled(
            f.room().getId(), f.settlement().getId(), host(), false);

        // 取り消したのは1件だけで、もう1件が精算済みのまま残るので locked は true
        assertThat(response.locked()).isTrue();
        assertThat(storedSettledAt(f.settlement().getId())).isNull();
        assertThat(storedSettledAt(stillSettled.getId())).isEqualTo(SETTLED_AT);
    }

    @Test
    void updateSettledThrowsSettlementNotFoundForUnknownSettlement() {
        Fixture f = fixture("存在しない送金案");

        assertThatThrownBy(() -> service.updateSettled(f.room().getId(), UUID.randomUUID(), host(), true))
            .isInstanceOf(SettlementNotFoundException.class);
    }

    @Test
    void updateSettledThrowsSettlementNotFoundForOtherRoomsSettlement() {
        Fixture f = fixture("対象ルーム");
        Fixture other = fixture("別ルーム");

        assertThatThrownBy(() -> service.updateSettled(
                f.room().getId(), other.settlement().getId(), host(), true))
            .isInstanceOf(SettlementNotFoundException.class);
        assertThat(storedSettledAt(other.settlement().getId())).isNull();
    }

    @Test
    void updateSettledThrowsRoomNotFoundForUnknownRoom() {
        assertThatThrownBy(() -> service.updateSettled(UUID.randomUUID(), UUID.randomUUID(), host(), true))
            .isInstanceOf(RoomNotFoundException.class);
    }

    // ---- resetSettled ----

    @Test
    void resetSettledClearsAllSettledAtKeepsTransfersAndDoesNotTouchOtherRooms() {
        Fixture f = fixture("リセット");
        f.settlement().markSettled(SETTLED_AT);
        settlements.save(f.settlement());
        Settlement secondSettled = settledSettlement(f.room(), f.other(), f.receiver(), 500);
        Fixture other = fixture("別ルーム");
        other.settlement().markSettled(SETTLED_AT);
        settlements.save(other.settlement());

        SettlementResponse response = service.resetSettled(f.room().getId());

        assertThat(response.locked()).isFalse();
        assertThat(response.transfers())
            .extracting(TransferSummary::id)
            .containsExactlyInAnyOrder(f.settlement().getId(), secondSettled.getId());
        assertThat(response.transfers()).allSatisfy(t -> assertThat(t.settledAt()).isNull());
        // 送金案は消えず、settledAt だけが NULL に戻る。別ルームの精算済みはそのまま
        assertThat(storedSettledAt(f.settlement().getId())).isNull();
        assertThat(storedSettledAt(secondSettled.getId())).isNull();
        assertThat(storedSettledAt(other.settlement().getId())).isEqualTo(SETTLED_AT);
    }

    @Test
    void resetSettledDoesNothingWhenNoneSettled() {
        Fixture f = fixture("精算済みなしのリセット");

        SettlementResponse response = service.resetSettled(f.room().getId());

        assertThat(response.locked()).isFalse();
        assertThat(response.transfers())
            .extracting(TransferSummary::id, TransferSummary::settledAt)
            .containsExactly(tuple(f.settlement().getId(), null));
        assertThat(settlements.findByRoomId(f.room().getId())).hasSize(1);
    }

    @Test
    void resetSettledThrowsRoomNotFoundForUnknownRoom() {
        assertThatThrownBy(() -> service.resetSettled(UUID.randomUUID()))
            .isInstanceOf(RoomNotFoundException.class);
    }
}
