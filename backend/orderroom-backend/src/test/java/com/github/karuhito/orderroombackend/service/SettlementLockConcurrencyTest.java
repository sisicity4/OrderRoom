package com.github.karuhito.orderroombackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.karuhito.orderroombackend.dto.UpdateItemStatusRequest;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.exception.SettlementLockedException;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;
import com.github.karuhito.orderroombackend.resolver.Operator;
import com.github.karuhito.orderroombackend.resolver.OperatorType;

/*
 * 精算済みへの変更と商品の更新が重なったとき、ルームのロックで直列化されることのテスト。
 * スレッドごとに別のトランザクションでコミットするため、テスト後にロールバックしない（データは開発用DBに残る）
 */
@SpringBootTest
class SettlementLockConcurrencyTest {
    @Autowired SettlementService settlementService;
    @Autowired ItemService itemService;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired ItemRepository items;
    @Autowired SettlementRepository settlements;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void itemUpdateWaitsForSettlingAndIsRejectedAfterCommit() throws Exception {
        Room room = rooms.save(new Room("同時実行の変更ロック"));
        Participant a = participants.save(new Participant(room, "A"));
        Participant b = participants.save(new Participant(room, "B"));
        Item item = items.save(new Item(room, a, "肉", 2000, 1, null));
        Settlement settlement = settlements.save(new Settlement(room, b, a, 1000));
        Operator host = new Operator(OperatorType.HOST, null);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch settled = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            // 精算済みにした後、コミットせずにロックを持ったまま止まる
            var holder = executor.submit(() -> transaction.executeWithoutResult(status -> {
                settlementService.updateSettled(room.getId(), settlement.getId(), host, true);
                settled.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("release timeout");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }));
            try {
                assertThat(settled.await(5, TimeUnit.SECONDS)).isTrue();
                var waiting = executor.submit(() -> {
                    started.countDown();
                    return itemService.updateStatus(room.getId(), item.getId(),
                        new UpdateItemStatusRequest(ItemStatus.REJECTED));
                });
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                // 精算済みへの変更がコミットされるまで、商品の更新はロックで待たされる
                assertThatThrownBy(() -> waiting.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
                release.countDown();
                holder.get(10, TimeUnit.SECONDS);
                // コミット後の精算済みを見て 409 になり、商品は変わらない
                assertThatThrownBy(() -> waiting.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(SettlementLockedException.class);
                assertThat(items.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(ItemStatus.PROPOSED);
                assertThat(settlements.findById(settlement.getId()).orElseThrow().getSettledAt()).isNotNull();
            } finally {
                release.countDown();
            }
        }
    }
}
