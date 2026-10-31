package com.github.karuhito.orderroombackend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.github.karuhito.orderroombackend.TestcontainersConfiguration;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Settlement;

import jakarta.persistence.EntityManager;

/* settlements テーブルへの保存・取得・置き換え（テスト後にロールバックするので、DBにデータは残らない） */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SettlementRepositoryTest {
    @Autowired SettlementRepository settlements;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired EntityManager entityManager;

    private static final Instant SETTLED_AT = Instant.parse("2026-01-01T00:00:00Z");

    /* 精算済みの送金案を作る */
    private Settlement settledSettlement(Room room, Participant from, Participant to, int amount) {
        Settlement settlement = new Settlement(room, from, to, amount);
        settlement.markSettled(SETTLED_AT);
        return settlements.save(settlement);
    }

    @Test
    void savesWithGeneratedIdAndCreatedAtAndNullSettledAt() {
        Room room = rooms.save(new Room("保存"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));

        Settlement saved = settlements.saveAndFlush(new Settlement(room, a, b, 3000));
        entityManager.clear();

        Settlement found = settlements.findById(saved.getId()).orElseThrow();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getSettledAt()).isNull();
        assertThat(found.getAmount()).isEqualTo(3000);
        assertThat(found.getRoom().getId()).isEqualTo(room.getId());
        assertThat(found.getFromParticipant().getId()).isEqualTo(a.getId());
        assertThat(found.getToParticipant().getId()).isEqualTo(b.getId());
    }

    @Test
    void findByRoomIdReturnsOnlyThatRoomsSettlements() {
        Room room = rooms.save(new Room("対象ルーム"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Room other = rooms.save(new Room("別ルーム"));
        Participant c = participants.save(new Participant(other, "C"));
        Participant d = participants.save(new Participant(other, "D"));
        Settlement unsettled = settlements.save(new Settlement(room, a, b, 1000));
        Settlement settled = settledSettlement(room, b, a, 500);
        settlements.save(new Settlement(other, c, d, 2000));

        assertThat(settlements.findByRoomId(room.getId()))
            .extracting(Settlement::getId)
            .containsExactlyInAnyOrder(unsettled.getId(), settled.getId());
    }

    @Test
    void findByRoomIdAndSettledAtIsNullReturnsOnlyUnsettled() {
        Room room = rooms.save(new Room("未精算の取得"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Settlement unsettled = settlements.save(new Settlement(room, a, b, 1000));
        settledSettlement(room, b, a, 500);

        assertThat(settlements.findByRoomIdAndSettledAtIsNull(room.getId()))
            .extracting(Settlement::getId)
            .containsExactly(unsettled.getId());
    }

    @Test
    void deleteByRoomIdAndSettledAtIsNullKeepsSettledAndOtherRooms() {
        Room room = rooms.save(new Room("置き換え"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Room other = rooms.save(new Room("別ルーム"));
        Participant c = participants.save(new Participant(other, "C"));
        Participant d = participants.save(new Participant(other, "D"));
        settlements.save(new Settlement(room, a, b, 1000));
        settlements.save(new Settlement(room, a, b, 2000));
        Settlement settled = settledSettlement(room, b, a, 500);
        Settlement otherUnsettled = settlements.save(new Settlement(other, c, d, 3000));
        entityManager.flush();

        settlements.deleteByRoomIdAndSettledAtIsNull(room.getId());
        entityManager.flush();
        entityManager.clear();

        // 対象ルームは精算済みの1件だけが残り、別ルームの未精算は消えない
        assertThat(settlements.findByRoomId(room.getId()))
            .extracting(Settlement::getId).containsExactly(settled.getId());
        assertThat(settlements.findByRoomId(other.getId()))
            .extracting(Settlement::getId).containsExactly(otherUnsettled.getId());
    }

    @Test
    void returnsEmptyListWhenRoomHasNoSettlements() {
        Room room = rooms.save(new Room("送金案なし"));

        assertThat(settlements.findByRoomId(room.getId())).isEmpty();
        assertThat(settlements.findByRoomIdAndSettledAtIsNull(room.getId())).isEmpty();
    }

    @Test
    void existsSettledReturnsTrueWhenRoomHasAtLeastOneSettled() {
        Room room = rooms.save(new Room("精算済みあり"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        settlements.save(new Settlement(room, a, b, 1000));
        settledSettlement(room, b, a, 500);

        assertThat(settlements.existsByRoomIdAndSettledAtIsNotNull(room.getId())).isTrue();
    }

    @Test
    void existsSettledReturnsFalseWhenAllUnsettled() {
        Room room = rooms.save(new Room("全件未精算"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        settlements.save(new Settlement(room, a, b, 1000));
        settlements.save(new Settlement(room, b, a, 500));

        assertThat(settlements.existsByRoomIdAndSettledAtIsNotNull(room.getId())).isFalse();
    }

    @Test
    void existsSettledReturnsFalseWhenOnlyOtherRoomHasSettled() {
        Room room = rooms.save(new Room("対象ルーム"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        settlements.save(new Settlement(room, a, b, 1000));
        Room other = rooms.save(new Room("別ルーム"));
        Participant c = participants.save(new Participant(other, "C"));
        Participant d = participants.save(new Participant(other, "D"));
        settledSettlement(other, c, d, 2000);

        assertThat(settlements.existsByRoomIdAndSettledAtIsNotNull(room.getId())).isFalse();
    }

    @Test
    void findByIdAndRoomIdReturnsSettlementWhenRoomMatches() {
        Room room = rooms.save(new Room("組で取得"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Settlement saved = settlements.save(new Settlement(room, a, b, 1000));

        assertThat(settlements.findByIdAndRoomId(saved.getId(), room.getId()))
            .get().extracting(Settlement::getId).isEqualTo(saved.getId());
    }

    @Test
    void findByIdAndRoomIdReturnsEmptyWhenRoomDiffers() {
        Room room = rooms.save(new Room("所属ルーム"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Settlement saved = settlements.save(new Settlement(room, a, b, 1000));
        Room other = rooms.save(new Room("別ルーム"));

        assertThat(settlements.findByIdAndRoomId(saved.getId(), other.getId())).isEmpty();
    }

    @Test
    void markSettledPersistsSettledAt() {
        Room room = rooms.save(new Room("精算済みにする"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Settlement saved = settlements.saveAndFlush(new Settlement(room, a, b, 1000));

        saved.markSettled(SETTLED_AT);
        settlements.saveAndFlush(saved);
        entityManager.clear();

        assertThat(settlements.findById(saved.getId()).orElseThrow().getSettledAt()).isEqualTo(SETTLED_AT);
    }

    @Test
    void markUnsettledPersistsNullSettledAt() {
        Room room = rooms.save(new Room("取り消す"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Settlement saved = settledSettlement(room, a, b, 1000);
        settlements.flush();

        saved.markUnsettled();
        settlements.saveAndFlush(saved);
        entityManager.clear();

        assertThat(settlements.findById(saved.getId()).orElseThrow().getSettledAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsAmountLessThanOne(int invalidAmount) {
        Room room = rooms.save(new Room("金額の制約"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));

        assertThatThrownBy(() -> settlements.saveAndFlush(new Settlement(room, a, b, invalidAmount)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
