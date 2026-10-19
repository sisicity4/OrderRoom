package com.github.karuhito.orderroombackend.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.karuhito.orderroombackend.dto.UpdateItemPurchaseDetailRequest;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.exception.ItemNotFoundException;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class ItemControllerUpdatePurchaseDetailTest {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private RoomRepository roomRepository;

  @Autowired private ParticipantRepository participantRepository;

  @Autowired private ItemRepository itemRepository;

  // 正常系1: acceptedかつpurchasedの商品に、実購入金額と購入者を登録
  @Test
  void updateItemPurchaseDetailTest() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidParticipant);
    UUID paidParticipantId = paidParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 200, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(150, paidParticipantId);
    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("コーラ"))
        .andExpect(jsonPath("$.actualPrice").value(150))
        .andExpect(jsonPath("$.paidByParticipantId").value(paidParticipant.getId().toString()))
        .andExpect(jsonPath("$.paidByParticipantName").value(paidParticipant.getName()));
  }

  @Test // 正常系2: 登録済みの商品へ別の金額・購入者で上書き
  void updateItemPurchaseDetailOverwriteTest() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    item.setActualPrice(200);
    item.setPaidByParticipant(participant);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.actualPrice").value(300))
        .andExpect(jsonPath("$.paidByParticipantId").value(paidByParticipantId.toString()))
        .andExpect(jsonPath("$.paidByParticipantName").value(paidByParticipant.getName()));

    Item checkItem =
        itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
    assertEquals(300, checkItem.getActualPrice());
    assertEquals(paidByParticipantId, checkItem.getPaidByParticipant().getId());
  }

  @Test // 正常系3: actualPriceとpaidByParticipantが両方nullでクリア(入力済みの商品)
  void updateItemPurchaseDetailClearTest() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    item.setActualPrice(200);
    item.setPaidByParticipant(participant);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(null, null);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("コーラ"))
        .andExpect(jsonPath("$.actualPrice").value(nullValue()))
        .andExpect(jsonPath("$.paidByParticipantId").value(nullValue()))
        .andExpect(jsonPath("$.paidByParticipantName").value(nullValue()));

    Item checkItem =
        itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
    assertNull(checkItem.getActualPrice());
    assertNull(checkItem.getPaidByParticipant());
  }

  @Test // 正常系4: actualPriceとpaidByParticipantが両方nullでクリア(状態が合わない商品)
  void updateItemPurchaseDetailClearInvalidStatusNoopTest() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(null, null);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("コーラ"))
        .andExpect(jsonPath("$.actualPrice").value(nullValue()))
        .andExpect(jsonPath("$.paidByParticipantId").value(nullValue()))
        .andExpect(jsonPath("$.paidByParticipantName").value(nullValue()));

    Item checkItem =
        itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
    assertNull(checkItem.getActualPrice());
    assertNull(checkItem.getPaidByParticipant());
  }

  @Test // 境界系: actualPriceを0に更新するとき
  void updateItemPurchaseDetailZeroPriceTest() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    item.setActualPrice(200);
    item.setPaidByParticipant(participant);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(0, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.actualPrice").value(0))
        .andExpect(jsonPath("$.paidByParticipantId").value(paidByParticipantId.toString()))
        .andExpect(jsonPath("$.paidByParticipantName").value(paidByParticipant.getName()));

    Item checkItem =
        itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
    assertEquals(0, checkItem.getActualPrice());
    assertEquals(paidByParticipantId, checkItem.getPaidByParticipant().getId());
  }

  // 異常系1: 認証(X-Host-Key)なし
  @Test
  void updatePurchaseDetailMissingHostKey() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidParticipant);
    UUID paidParticipantId = paidParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 200, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(150, paidParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("ホストキーが無効です"));
  }

  @Test // 異常系2: proposedの商品へ値ありでリクエストを送る
  void updatePurchaseDetailProposedItem() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.PROPOSED);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ITEM_STATE"))
        .andExpect(jsonPath("$.message").value("アイテムを採用済みにしている必要があります"));
  }

  @Test // 異常系3: rejectedの商品へ値ありでリクエストを送る
  void updatePurchaseDetailRejectedItem() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.REJECTED);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ITEM_STATE"))
        .andExpect(jsonPath("$.message").value("アイテムを採用済みにしている必要があります"));
  }

  @Test // 異常系4: acceptedだがpurchasedがfalseの商品へ値ありでリクエストを送る
  void updatePurchaseDetailNotPurchasedItem() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ITEM_STATE"))
        .andExpect(jsonPath("$.message").value("アイテムを購入済みにしている必要があります"));
  }

  @Test // 異常系5: purchasedがfalseかつ他のルームの参加者を購入者にしようとしている
  void updatePurchaseDetailInvalidStateWithOtherRoomParticipant() throws Exception {
    Room room1 = new Room("テストルーム");
    roomRepository.save(room1);
    UUID roomId = room1.getId();
    UUID hostKey = room1.getHostKey();

    Participant participant = new Participant(room1, "テストユーザー1");
    participantRepository.save(participant);

    // 別ルームと別ルームの参加者を作成
    Room room2 = new Room("別ルーム");
    roomRepository.save(room2);
    Participant paidByParticipant = new Participant(room2, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room1, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ITEM_STATE"))
        .andExpect(jsonPath("$.message").value("アイテムを購入済みにしている必要があります"));
  }

  @Test // 他ルームの参加者を購入者に指定
  void updatePurchasedDetailOtherRoomParticipant() throws Exception {
    Room room1 = new Room("テストルーム");
    roomRepository.save(room1);
    UUID roomId = room1.getId();
    UUID hostKey = room1.getHostKey();

    Participant participant = new Participant(room1, "テストユーザー1");
    participantRepository.save(participant);

    // 別ルームと別ルームの参加者を作成
    Room room2 = new Room("別ルーム");
    roomRepository.save(room2);
    Participant paidByParticipant = new Participant(room2, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room1, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("PARTICIPANT_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("参加者IDが正しくありません"));
  }

  @Test // 存在しない参加者を購入者に指定
  void updatePurchasedDetailParticipantNotFound() throws Exception {
    Room room1 = new Room("テストルーム");
    roomRepository.save(room1);
    UUID roomId = room1.getId();
    UUID hostKey = room1.getHostKey();

    Participant participant = new Participant(room1, "テストユーザー1");
    participantRepository.save(participant);

    // 実在しない購入者ID
    UUID paidByParticipantId = UUID.randomUUID();

    Item item = new Item(room1, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("PARTICIPANT_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("参加者IDが正しくありません"));
  }

  @Test // 入力検証1: actualPriceのみを送る（購入者なし）
  void updatePurchaseDetailOnlyActualPrice() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(300, null);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.paidByParticipantId").value("実購入額を入力した場合は購入者も選択してください"));
  }

  @Test // 入力検証2: 購入者のみを送る（actualPriceなし）
  void updatePurchaseDetailOnlyPaidByParticipant() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(null, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.actualPrice").value("購入者を選択した場合は実購入額も入力してください"));
  }

  @Test // 入力検証3: actualPriceがマイナスの場合
  void updatePurchaseDetailNegativeActualPrice() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(-1, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.actualPrice").value("実購入額をマイナスに設定することはできません"));
  }

  @Test // 存在確認1: 存在しないitemIdの場合
  void updatePurchaseDetailItemNotFound() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant paidByParticipant = new Participant(room, "テストユーザー1");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    // 実在しないitemId
    UUID itemId = UUID.randomUUID();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("アイテムが見つかりません"));
  }

  @Test // 存在確認2: 別ルームのitemIdの場合
  void updatePurchaseDetailItemOfAnotherRoom() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();
    UUID hostKey = room.getHostKey();

    Participant paidByParticipant = new Participant(room, "テストユーザー1");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    // 別ルームと、そのルームのアイテムを作成
    Room anotherRoom = new Room("別ルーム");
    roomRepository.save(anotherRoom);
    Participant anotherParticipant = new Participant(anotherRoom, "別ルーム参加者");
    participantRepository.save(anotherParticipant);

    Item item = new Item(anotherRoom, anotherParticipant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("ITEM_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("アイテムが見つかりません"));
  }

  @Test // 認証1: X-Host-Keyが不一致の場合
  void updatePurchaseDetailMismatchHostKey() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();

    // room.getHostKey()とは異なるホストキー
    UUID hostKey = UUID.randomUUID();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("ホストキーが無効です"));
  }

  @Test // 認証2: 別ルームのX-Host-Keyを送る場合
  void updatePurchaseDetailAnotherRoomHostKey() throws Exception {
    Room room = new Room("テストルーム");
    roomRepository.save(room);
    UUID roomId = room.getId();

    // 別ルームのホストキー
    Room anotherRoom = new Room("別ルーム");
    roomRepository.save(anotherRoom);
    UUID anotherHostKey = anotherRoom.getHostKey();

    Participant participant = new Participant(room, "テストユーザー1");
    participantRepository.save(participant);

    Participant paidByParticipant = new Participant(room, "テストユーザー2");
    participantRepository.save(paidByParticipant);
    UUID paidByParticipantId = paidByParticipant.getId();

    Item item = new Item(room, participant, "コーラ", 150, 1, null);
    item.setStatus(ItemStatus.ACCEPTED);
    item.setPurchased(true);
    itemRepository.save(item);
    UUID itemId = item.getId();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, paidByParticipantId);

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", roomId, itemId)
                .header("X-Host-Key", anotherHostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("ホストキーが無効です"));
  }

  @Test // 型: roomIdがUUIDではない場合
  void updatePurchaseDetailInvalidRoomId() throws Exception {
    UUID itemId = UUID.randomUUID();
    UUID hostKey = UUID.randomUUID();

    UpdateItemPurchaseDetailRequest request =
        new UpdateItemPurchaseDetailRequest(300, UUID.randomUUID());

    mockMvc
        .perform(
            patch("/api/rooms/{roomId}/items/{itemId}/purchase-detail", "not-a-uuid", itemId)
                .header("X-Host-Key", hostKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
        .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
        .andExpect(jsonPath("$.fields.roomId").value("不正な値: not-a-uuid"));
  }
}
