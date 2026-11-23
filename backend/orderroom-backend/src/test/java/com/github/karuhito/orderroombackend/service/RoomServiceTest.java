package com.github.karuhito.orderroombackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;

import com.github.karuhito.orderroombackend.TestcontainersConfiguration;
import com.github.karuhito.orderroombackend.dto.CreateRoomRequest;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

/*
 * ルーム作成と作成者の参加者登録が、1つのトランザクションで行われることを確かめる（#141）
 * テストメソッドに @Transactional を付けると RoomService のトランザクションがテストのものに合流してしまうので、付けない
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class RoomServiceTest {
    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Test // 参加者の保存に失敗したら、ルームも保存されない
    void roomIsNotSavedWhenParticipantSaveFails() {
        // 他のテストのルームと区別するため、タイトルを一意にする
        String title = "ロールバック確認-" + UUID.randomUUID();
        // DTO のバリデーション（50字以内）は Controller で効くので、Service を直接呼ぶと DB の varchar(50) で初めて失敗する
        CreateRoomRequest request = new CreateRoomRequest(title, null, null, null, "あ".repeat(51));

        assertThatThrownBy(() -> roomService.createRoom(request))
            .isInstanceOf(DataAccessException.class);

        assertThat(roomRepository.findAll())
            .noneMatch(room -> title.equals(room.getTitle()));
    }
}
