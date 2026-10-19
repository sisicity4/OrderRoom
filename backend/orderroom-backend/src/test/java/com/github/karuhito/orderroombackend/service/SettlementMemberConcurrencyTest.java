package com.github.karuhito.orderroombackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class SettlementMemberConcurrencyTest {
  @Autowired SettlementMemberService service;
  @Autowired RoomRepository rooms;
  @Autowired ParticipantRepository participants;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  void concurrentReplacementsNeverMixSelections() throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      for (int round = 0; round < 10; round++) {
        Room room = rooms.save(new Room("同時置換"));
        List<UUID> first =
            List.of(
                participants.save(new Participant(room, "A")).getId(),
                participants.save(new Participant(room, "B")).getId());
        List<UUID> second =
            List.of(
                participants.save(new Participant(room, "C")).getId(),
                participants.save(new Participant(room, "D")).getId());
        // 未設定と設定済みの両方を検証する。
        if (round % 2 == 1) service.replaceMembers(room.getId(), first);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var a =
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(5, TimeUnit.SECONDS))
                    throw new IllegalStateException("start timeout");
                  return service.replaceMembers(room.getId(), first);
                });
        var b =
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(5, TimeUnit.SECONDS))
                    throw new IllegalStateException("start timeout");
                  return service.replaceMembers(room.getId(), second);
                });
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(a.get(10, TimeUnit.SECONDS)).isEqualTo(first);
        assertThat(b.get(10, TimeUnit.SECONDS)).isEqualTo(second);
        List<UUID> actual =
            service.getEffectiveMembers(room.getId()).stream().map(Participant::getId).toList();
        assertThat(actual).hasSize(2);
        assertThat(actual.containsAll(first) || actual.containsAll(second)).isTrue();
      }
    }
  }

  @Test
  void replacementWaitsForRoomLockAndCompletesAfterCommit() throws Exception {
    Room room = rooms.save(new Room("ロック待機"));
    UUID a = participants.save(new Participant(room, "A")).getId();
    UUID b = participants.save(new Participant(room, "B")).getId();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch started = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var holder =
          executor.submit(
              () ->
                  transaction.executeWithoutResult(
                      status -> {
                        rooms.findByIdForUpdate(room.getId()).orElseThrow();
                        service.replaceMembers(room.getId(), List.of(a));
                        locked.countDown();
                        try {
                          if (!release.await(10, TimeUnit.SECONDS))
                            throw new IllegalStateException("release timeout");
                        } catch (InterruptedException ex) {
                          Thread.currentThread().interrupt();
                          throw new IllegalStateException(ex);
                        }
                      }));
      try {
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        var waiting =
            executor.submit(
                () -> {
                  started.countDown();
                  return service.replaceMembers(room.getId(), List.of(b));
                });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> waiting.get(300, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        release.countDown();
        holder.get(10, TimeUnit.SECONDS);
        assertThat(waiting.get(10, TimeUnit.SECONDS)).containsExactly(b);
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId)
            .containsExactly(b);
      } finally {
        release.countDown();
      }
    }
  }
}
