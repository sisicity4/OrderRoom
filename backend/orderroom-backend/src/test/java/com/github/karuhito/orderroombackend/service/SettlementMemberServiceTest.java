package com.github.karuhito.orderroombackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.exception.ParticipantNotFoundException;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementMemberRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SettlementMemberServiceTest {
  @Autowired SettlementMemberService service;
  @Autowired RoomRepository rooms;
  @Autowired ParticipantRepository participants;
  @Autowired SettlementMemberRepository members;

  @Test
  void defaultsToAllWithoutSavingAndIncludesLaterParticipants() {
    Room room = rooms.save(new Room("未設定"));
    Participant a = participants.save(new Participant(room, "A"));
    assertThat(service.getEffectiveMembers(room.getId()))
        .extracting(Participant::getId)
        .containsExactly(a.getId());
    Participant b = participants.save(new Participant(room, "B"));
    assertThat(service.getEffectiveMembers(room.getId()))
        .extracting(Participant::getId)
        .containsExactlyInAnyOrder(a.getId(), b.getId());
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
        .extracting(Participant::getId)
        .containsExactlyInAnyOrder(b.getId(), c.getId());
    assertThat(members.findByIdRoomId(room.getId()))
        .hasSize(2)
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
        .extracting(Participant::getId)
        .containsExactly(a.getId());
    assertThat(service.getEffectiveMembers(other.getId()))
        .extracting(Participant::getId)
        .containsExactly(outsider.getId());
  }

  @Test
  void rejectsMissingRoomEvenWhenReadingDefaults() {
    UUID missing = UUID.randomUUID();
    assertThatThrownBy(() -> service.getEffectiveMembers(missing))
        .isInstanceOf(RoomNotFoundException.class);
    assertThatThrownBy(() -> service.replaceMembers(missing, List.of(UUID.randomUUID())))
        .isInstanceOf(RoomNotFoundException.class);
  }
}
