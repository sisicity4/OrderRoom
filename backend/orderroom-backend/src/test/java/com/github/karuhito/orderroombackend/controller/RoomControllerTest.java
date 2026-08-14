package com.github.karuhito.orderroombackend.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.github.karuhito.orderroombackend.dto.CreateRoomRequest;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc

public class RoomControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ItemRepository itemRepository;

    // CreateRoom
    @Test // 正常系
    void createRoomTest() throws Exception {
        // リクエストDTOを組み立てる
        CreateRoomRequest request = new CreateRoomRequest("testTitle", null, null, null);

        // mockMvcでPOSTリクエストを送る
        mockMvc.perform(
                post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 検証
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("testTitle"))
                .andExpect(jsonPath("$.hostKey").exists())
                .andExpect(jsonPath("$.budgetAmount").isEmpty());
    }

    @Test // 異常系: 入力値が不正
    void createRoomValidTest() throws Exception {
        // 全てnull
        CreateRoomRequest request = new CreateRoomRequest(null, null, null, null);

        mockMvc.perform(
                post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALID_ERROR"))
                .andExpect(jsonPath("$.message").value("不正な入力です"))
                .andExpect(jsonPath("$.fields.title").value("空白は許可されていません"));
    }

    @Test // 正常系 budgetAmountを指定できる
    void createRoomWithBudgetAmountTest() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("テストルーム", null, null, 12000);

        mockMvc.perform(
                post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("テストルーム"))
                .andExpect(jsonPath("$.hostKey").exists())
                .andExpect(jsonPath("$.budgetAmount").value(12000));
    }

    @Test // 正常系 budgetAmountが0のパターン
    void createRoomZeroBudgetAmountTest() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("テストルーム", null, null, 0);

        mockMvc.perform(
                post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("テストルーム"))
                .andExpect(jsonPath("$.hostKey").exists())
                .andExpect(jsonPath("$.budgetAmount").value(0));
    }

    @Test // 異常系: budgetAmountが負の数値
    void createRoomInValidBudgetAmountTest() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("テストルーム", null, null, -1);

        mockMvc.perform(
                post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALID_ERROR"))
                .andExpect(jsonPath("$.message").value("不正な入力です"))
                .andExpect(jsonPath("$.fields.budgetAmount").value("予算上限をマイナスに設定することはできません"));
    }

    // アイテム集計のテスト
    @Test // 正常系
    void getItemSummary() throws Exception {
        // ルーム作成
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        // 参加者を3人作成
        Participant participant1 = new Participant(room, "テストユーザー1");
        participantRepository.save(participant1);

        Participant participant2 = new Participant(room, "テストユーザー2");
        participantRepository.save(participant2);

        Participant participant3 = new Participant(room, "テストユーザー3");
        participantRepository.save(participant3);

        // 同じ参加者でStatusがACCEPTEDのアイテムを2つ用意する
        // participant1 でアイテムを作成
        // 値段100 個数1 ステータスaccepted
        Item item1 = new Item(room, participant1, "テストアイテム1", 100, 1, null);
        item1.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item1);
        // 値段200 個数2 ステータスaccepted
        Item item2 = new Item(room, participant1, "テストアイテム2", 200, 2, null);
        item2.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item2);
        // 値段100 個数1 ステータスproposed
        Item item3 = new Item(room, participant1, "テストアイテム3", 100, 1, null);
        itemRepository.save(item3);
        // 値段100 個数1 ステータスrejected
        Item item4 = new Item(room, participant1, "テストアイテム4", 100, 1, null);
        item4.setStatus(ItemStatus.REJECTED);
        itemRepository.save(item4);

        // participant2 でアイテムを作成。
        // 同じ商品名で個数を3個にする
        // 名前がitem1と同じ 値段100 個数3 ステータスaccepted
        Item item5 = new Item(room, participant2, "テストアイテム1", 100, 3, null);
        item5.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item5);
        // 値段100 個数1 ステータスproposed
        Item item6 = new Item(room, participant2, "テストアイテム5", 100, 1, null);
        itemRepository.save(item6);

        // participant3はREJECTEDのアイテムのみを持つ
        // 値段100 個数3
        Item item7 = new Item(room, participant3, "テストアイテム6", 100, 3, null);
        item7.setStatus(ItemStatus.REJECTED);
        itemRepository.save(item7);
        // 値段100 個数1
        Item item8 = new Item(room, participant3, "テストアイテム7", 100, 1, null);
        item8.setStatus(ItemStatus.REJECTED);
        itemRepository.save(item8);

        mockMvc.perform(
                get("/api/rooms/{roomId}/summary", roomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedTotalPrice").value(800))
                .andExpect(jsonPath("$.proposedTotalPrice").value(200))
                .andExpect(jsonPath("$.budgetAmount").isEmpty())
                .andExpect(jsonPath("$.remainingBudget").isEmpty())
                .andExpect(jsonPath("$.overBudgetAmount").isEmpty())
                .andExpect(jsonPath("$.acceptedItemCount").value(3))
                // byStatusでステータスごとの件数を確認
                .andExpect(jsonPath("$.byStatus.accepted").value(3))
                .andExpect(jsonPath("$.byStatus.proposed").value(2))
                .andExpect(jsonPath("$.byStatus.rejected").value(3))
                // 参加者ごとの集計
                // 参加者の数の確認
                .andExpect(jsonPath("$.perParticipant.length()").value(3))
                // participant1 4件の提案 acceptedの合計金額500
                .andExpect(jsonPath("$.perParticipant[0].participantId").value(participant1.getId().toString()))
                .andExpect(jsonPath("$.perParticipant[0].name").value("テストユーザー1"))
                .andExpect(jsonPath("$.perParticipant[0].proposalCount").value(4))
                .andExpect(jsonPath("$.perParticipant[0].acceptedTotalPrice").value(500))
                // participant3 2件の提案 acceptedの合計は0
                .andExpect(jsonPath("$.perParticipant[2].proposalCount").value(2))
                .andExpect(jsonPath("$.perParticipant[2].acceptedTotalPrice").value(0))

                // rejectedを除いたアイテムの名前ごとの集計
                // rejected除外の確認
                .andExpect(jsonPath("$.itemQuantities.length()").value(4))
                // 合算の確認
                .andExpect(jsonPath("$.itemQuantities[0].name").value("テストアイテム1"))
                .andExpect(jsonPath("$.itemQuantities[0].totalQuantity").value(4))
                .andExpect(jsonPath("$.itemQuantities[0].estimatedTotalPrice").value(400))
                // proposedのみの商品を1件
                .andExpect(jsonPath("$.itemQuantities[2].name").value("テストアイテム3"))
                .andExpect(jsonPath("$.itemQuantities[2].totalQuantity").value(1))
                .andExpect(jsonPath("$.itemQuantities[2].estimatedTotalPrice").value(100))
                // 最後の要素を取得して商品名昇順になっているか確認
                .andExpect(jsonPath("$.itemQuantities[3].name").value("テストアイテム5"));
    }

    @Test // roomIdが見つからない
    void getItemSummaryNotFoundRoom() throws Exception {
        UUID roomId = UUID.randomUUID();

        mockMvc.perform(
                get("/api/rooms/{roomId}/summary", roomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("ルームが見つかりません"));
    }

    @Test // アイテムが存在しない場合
    void getItemSummaryEmptyList() throws Exception {
        Room room = new Room("テストルーム");
        roomRepository.save(room);
        UUID roomId = room.getId();

        mockMvc.perform(
                get("/api/rooms/{roomId}/summary", roomId)

        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedTotalPrice").value(0))
                .andExpect(jsonPath("$.proposedTotalPrice").value(0))
                .andExpect(jsonPath("$.budgetAmount").isEmpty())
                .andExpect(jsonPath("$.remainingBudget").isEmpty())
                .andExpect(jsonPath("$.overBudgetAmount").isEmpty())
                .andExpect(jsonPath("$.acceptedItemCount").value(0))
                .andExpect(jsonPath("$.byStatus.accepted").value(0))
                .andExpect(jsonPath("$.byStatus.proposed").value(0))
                .andExpect(jsonPath("$.byStatus.rejected").value(0))
                .andExpect(jsonPath("$.perParticipant").isEmpty())
                .andExpect(jsonPath("$.itemQuantities").isEmpty());
    }

    @Test // 予算内の場合 budgetAmountが設定値、remainingBudgetが残額、overBudgetAmount = 0になる
    void getItemSummaryInBudgetAmount() throws Exception {
        // ルーム作成 予算は1000
        Room room = new Room("テストルーム");
        room.setBudgetAmount(1000);
        roomRepository.save(room);
        UUID roomId = room.getId();

        // 参加者を作成
        Participant participant = new Participant(room, "テストユーザー");
        participantRepository.save(participant);

        // アイテムを2件作成
        // 値段100 個数1 ステータスproposed
        Item item1 = new Item(room, participant, "テストアイテム1", 100, 1, null);
        item1.setStatus(ItemStatus.PROPOSED);
        itemRepository.save(item1);
        // 値段200 個数2 ステータスaccepted
        Item item2 = new Item(room, participant, "テストアイテム2", 200, 2, null);
        item2.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item2);

        mockMvc.perform(
                get("/api/rooms/{roomId}/summary", roomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(1000))
                // item1の値段は残額から引かれていないことを確認
                .andExpect(jsonPath("$.remainingBudget").value(600))
                .andExpect(jsonPath("$.overBudgetAmount").value(0));
    }

    @Test // 予算とちょうどの場合
    void getItemSummaryJustBudgetAmount() throws Exception {
        // ルーム作成 予算は1000
        Room room = new Room("テストルーム");
        room.setBudgetAmount(1000);
        roomRepository.save(room);
        UUID roomId = room.getId();

        // 参加者を作成
        Participant participant = new Participant(room, "テストユーザー");
        participantRepository.save(participant);

        // アイテムを2件作成
        // 値段100 個数6 ステータスaccepted
        Item item1 = new Item(room, participant, "テストアイテム1", 100, 6, null);
        item1.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item1);
        // 値段200 個数2 ステータスaccepted
        Item item2 = new Item(room, participant, "テストアイテム2", 200, 2, null);
        item2.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item2);

        mockMvc.perform(
                get("/api/rooms/{roomId}/summary", roomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(1000))
                // 予算ちょうどなのでremainingBudget と overBudgetが0になることを確認
                .andExpect(jsonPath("$.remainingBudget").value(0))
                .andExpect(jsonPath("$.overBudgetAmount").value(0));
    }

    @Test // 予算超過の場合 remainingBudget = 0、 overBudgetAmountが超えた分の数値になることを確認
    void getItemSummaryOverBudgetAmount() throws Exception {
        // ルーム作成 予算は1000
        Room room = new Room("テストルーム");
        room.setBudgetAmount(1000);
        roomRepository.save(room);
        UUID roomId = room.getId();

        // 参加者を作成
        Participant participant = new Participant(room, "テストユーザー");
        participantRepository.save(participant);

        // アイテムを2件作成
        // 値段100 個数1 ステータスaccepted
        Item item1 = new Item(room, participant, "テストアイテム1", 100, 1, null);
        item1.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item1);
        // 値段200 個数5 ステータスaccepted
        Item item2 = new Item(room, participant, "テストアイテム2", 200, 5, null);
        item2.setStatus(ItemStatus.ACCEPTED);
        itemRepository.save(item2);

        mockMvc.perform(
                get("/api/rooms/{roomId}/summary", roomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // overBudgetAmountが100になることを確認
                .andExpect(jsonPath("$.budgetAmount").value(1000))
                .andExpect(jsonPath("$.remainingBudget").value(0))
                .andExpect(jsonPath("$.overBudgetAmount").value(100));
    }

    // getRoomテスト
    @Test // 正常系1 hostKeyがResponseに含まれていないことを確認する
    void getRoom() throws Exception {
        Room room = new Room("テストルーム");
        room.setBudgetAmount(20000);
        room.setMemo("テストメモ");
        room.setEventDate(LocalDate.of(2000, 1, 1));
        roomRepository.save(room);
        UUID roomId = room.getId();

        mockMvc.perform(
                get("/api/rooms/{roomId}", roomId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(room.getId().toString()))
                .andExpect(jsonPath("$.title").value("テストルーム"))
                .andExpect(jsonPath("$.eventDate").value("2000-01-01"))
                .andExpect(jsonPath("$.memo").value("テストメモ"))
                .andExpect(jsonPath("$.budgetAmount").value(20000))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.hostKey").doesNotExist());
    }

    @Test // 正常系2 budgetAmountが未設定の場合でも200で通る
    void getRoomNullBudgetAmount() throws Exception {
        Room room = new Room("テストルーム");
        room.setMemo("テストメモ");
        room.setEventDate(LocalDate.of(2000, 1, 1));
        roomRepository.save(room);
        UUID roomId = room.getId();

        mockMvc.perform(
                get("/api/rooms/{roomId}", roomId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").isEmpty());
    }

    @Test // 異常系1 roomIdが存在しない
    void getRoomNotFoundRoom() throws Exception {
        UUID roomId = UUID.randomUUID();

        mockMvc.perform(
                get("/api/rooms/{roomId}", roomId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("ルームが見つかりません"));
    }

}
