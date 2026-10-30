package com.github.karuhito.orderroombackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.exception.ParticipantNotFoundException;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.exception.SettlementLockedException;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementMemberRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;

@SpringBootTest
class SettlementMemberServiceTest {
    @Autowired SettlementMemberService service;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired SettlementMemberRepository members;
    @Autowired SettlementRepository settlements;

    /* 精算済みの送金案を1件保存する */
    private void saveSettled(Room room, Participant from, Participant to) {
        Settlement settlement = new Settlement(room, from, to, 1000);
        settlement.markSettled(Instant.parse("2026-02-01T00:00:00Z"));
        settlements.save(settlement);
    }

    @Test
    void defaultsToAllWithoutSavingAndIncludesLaterParticipants() {
        Room room = rooms.save(new Room("未設定"));
        Participant a = participants.save(new Participant(room, "A"));
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactly(a.getId());
        Participant b = participants.save(new Participant(room, "B"));
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(members.findByIdRoomId(room.getId())).isEmpty();
    }

    @Test
    void replacesAndCanRepeatTheSameSelection() {
        Room room = rooms.save(new Room("置換"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Participant c = participants.save(new Participant(room, "C"));
        service.replaceMembers(room.getId(), List.of(a.getId(), b.getId()));
        assertThat(service.replaceMembers(room.getId(), List.of(c.getId(), b.getId())))
            .containsExactly(c.getId(), b.getId());
        service.replaceMembers(room.getId(), List.of(b.getId(), c.getId()));
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactlyInAnyOrder(b.getId(), c.getId());
        assertThat(members.findByIdRoomId(room.getId())).hasSize(2)
            .allSatisfy(member -> assertThat(member.getCreatedAt()).isNotNull());
    }

    @Test
    void rejectsInvalidSelectionWithoutChangingExistingMembers() {
        Room room = rooms.save(new Room("対象ルーム"));
        Participant a = participants.save(new Participant(room, "A"));
        Room other = rooms.save(new Room("別ルーム"));
        Participant outsider = participants.save(new Participant(other, "B"));
        service.replaceMembers(room.getId(), List.of(a.getId()));
        service.replaceMembers(other.getId(), List.of(outsider.getId()));

        assertThatThrownBy(() -> service.replaceMembers(room.getId(), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.replaceMembers(room.getId(), List.of(outsider.getId())))
            .isInstanceOf(ParticipantNotFoundException.class);
        assertThatThrownBy(() -> service.replaceMembers(room.getId(), List.of(UUID.randomUUID())))
            .isInstanceOf(ParticipantNotFoundException.class);
        assertThatThrownBy(() -> service.replaceMembers(room.getId(), List.of(a.getId(), a.getId())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactly(a.getId());
        assertThat(service.getEffectiveMembers(other.getId()))
            .extracting(Participant::getId).containsExactly(outsider.getId());
    }

    @Test
    void rejectsMissingRoomEvenWhenReadingDefaults() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> service.getEffectiveMembers(missing))
            .isInstanceOf(RoomNotFoundException.class);
        assertThatThrownBy(() -> service.replaceMembers(missing, List.of(UUID.randomUUID())))
            .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void rejectsReplacingWhenAnySettledWithoutChangingMembers() {
        Room room = rooms.save(new Room("精算済みで対象者変更"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        service.replaceMembers(room.getId(), List.of(a.getId(), b.getId()));
        saveSettled(room, b, a);

        assertThatThrownBy(() -> service.replaceMembers(room.getId(), List.of(a.getId())))
            .isInstanceOf(SettlementLockedException.class)
            .hasMessage(room.getId().toString());
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactlyInAnyOrder(a.getId(), b.getId());
    }

    @Test
    void checksSettledBeforeParticipantExistence() {
        Room room = rooms.save(new Room("精算済みで不正な参加者"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        saveSettled(room, b, a);

        // 存在しない参加者IDでも、精算済みの 409 が 404 より先に出る
        assertThatThrownBy(() -> service.replaceMembers(room.getId(), List.of(UUID.randomUUID())))
            .isInstanceOf(SettlementLockedException.class);
    }

    @Test
    void replacesWhenOnlyOtherRoomIsSettled() {
        Room room = rooms.save(new Room("対象ルーム"));
        Participant a = participants.save(new Participant(room, "A"));
        participants.save(new Participant(room, "B"));
        Room other = rooms.save(new Room("別ルーム"));
        Participant c = participants.save(new Participant(other, "C"));
        Participant d = participants.save(new Participant(other, "D"));
        saveSettled(other, d, c);

        assertThat(service.replaceMembers(room.getId(), List.of(a.getId()))).containsExactly(a.getId());
    }
}
