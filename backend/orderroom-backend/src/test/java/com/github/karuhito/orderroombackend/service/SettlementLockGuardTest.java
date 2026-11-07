package com.github.karuhito.orderroombackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.github.karuhito.orderroombackend.TestcontainersConfiguration;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.exception.SettlementLockedException;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;

/* ルームのロック取得と精算済みの判定（ロックはトランザクション内でしか取れないため、テスト側で張る。テスト後にロールバックする） */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SettlementLockGuardTest {
    @Autowired SettlementLockGuard guard;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired SettlementRepository settlements;

    /* 精算済みの送金案を1件持つルームを作る */
    private Room roomWithSettled(String name) {
        Room room = rooms.save(new Room(name));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Settlement settled = new Settlement(room, a, b, 1000);
        settled.markSettled(Instant.parse("2026-01-01T00:00:00Z"));
        settlements.save(settled);
        return room;
    }

    @Test
    void lockRoomReturnsRoomWhenExists() {
        Room room = rooms.save(new Room("ロック"));

        assertThat(guard.lockRoom(room.getId()))
            .extracting(Room::getId).isEqualTo(room.getId());
    }

    @Test
    void lockRoomThrowsWhenRoomNotFound() {
        assertThatThrownBy(() -> guard.lockRoom(UUID.randomUUID()))
            .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void ensureNotSettledPassesWhenNoSettled() {
        Room room = rooms.save(new Room("未精算のみ"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        settlements.save(new Settlement(room, a, b, 1000));

        assertThatCode(() -> guard.ensureNotSettled(room.getId())).doesNotThrowAnyException();
    }

    @Test
    void ensureNotSettledThrowsWhenAtLeastOneSettled() {
        Room room = roomWithSettled("精算済みあり");

        assertThatThrownBy(() -> guard.ensureNotSettled(room.getId()))
            .isInstanceOf(SettlementLockedException.class)
            .hasMessage(room.getId().toString());
    }

    @Test
    void ensureNotSettledPassesWhenOnlyOtherRoomIsSettled() {
        Room room = rooms.save(new Room("対象ルーム"));
        roomWithSettled("別ルーム");

        assertThatCode(() -> guard.ensureNotSettled(room.getId())).doesNotThrowAnyException();
    }
}
