package com.github.karuhito.orderroombackend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.github.karuhito.orderroombackend.dto.ByStatus;
import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
import com.github.karuhito.orderroombackend.dto.CreateItemResponse;
import com.github.karuhito.orderroombackend.dto.ItemListResponse;
import com.github.karuhito.orderroombackend.dto.ItemNameSummary;
import com.github.karuhito.orderroombackend.dto.ItemSummaryResponse;
import com.github.karuhito.orderroombackend.dto.ParticipantSummary;
import com.github.karuhito.orderroombackend.dto.UpdateItemPurchasedRequest;
import com.github.karuhito.orderroombackend.dto.UpdateItemRequest;
import com.github.karuhito.orderroombackend.dto.UpdateItemStatusRequest;

import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;

import com.github.karuhito.orderroombackend.exception.ItemNotFoundException;
import com.github.karuhito.orderroombackend.exception.ItemStatusInvalidException;
import com.github.karuhito.orderroombackend.exception.NotItemOwnerException;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;

import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.resolver.Operator;

@Service

public class ItemService {
    private final ItemRepository itemRepository;
    private final RoomRepository roomRepository;

    public ItemService(ItemRepository itemRepository, RoomRepository roomRepository) {
        this.itemRepository = itemRepository;
        this.roomRepository = roomRepository;
    }

    public CreateItemResponse createItem(Participant participant, CreateItemRequest request) {
        Room room = participant.getRoom();
        Item item = new Item(room, participant, request.name(), request.price(), request.quantity(), request.memo());
        Item savedItem = itemRepository.save(item);
        return new CreateItemResponse(
                savedItem.getId(),
                savedItem.getRoom().getId(),
                savedItem.getParticipant().getId(),
                savedItem.getName(),
                savedItem.getPrice(),
                savedItem.getQuantity(),
                savedItem.getMemo(),
                savedItem.getStatus(),
                savedItem.isPurchased(),
                savedItem.getCreatedAt(),
                savedItem.getUpdatedAt());
    }

    public List<ItemListResponse> getItems(UUID roomId, ItemStatus status, UUID participantId) {
        roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
        List<Item> items = itemRepository.findItemsByRoomAndFilters(roomId, status, participantId);
        return items.stream()
                .map(item -> new ItemListResponse(item.getId(), item.getRoom().getId(), item.getParticipant().getId(),
                        item.getParticipant().getName(), item.getName(), item.getPrice(), item.getQuantity(),
                        item.getMemo(), item.getStatus(), item.isPurchased(), item.getCreatedAt(), item.getUpdatedAt()))
                .toList();
    }

    // ItemListResponseを再利用する
    public ItemListResponse updateStatus(UUID roomId, UUID itemId, UpdateItemStatusRequest request) {
        Item item = itemRepository.findByIdAndRoomId(itemId, roomId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));
        item.setStatus(request.status());
        // statusがACCEPTED以外になる場合、Purchasedをfalseにする
        if (!item.getStatus().equals(ItemStatus.ACCEPTED)) {
            item.setPurchased(false);
        }

        Item updatedItem = itemRepository.save(item);

        return new ItemListResponse(
                updatedItem.getId(),
                updatedItem.getRoom().getId(),
                updatedItem.getParticipant().getId(),
                updatedItem.getParticipant().getName(),
                updatedItem.getName(),
                updatedItem.getPrice(),
                updatedItem.getQuantity(),
                updatedItem.getMemo(),
                updatedItem.getStatus(),
                updatedItem.isPurchased(),
                updatedItem.getCreatedAt(),
                updatedItem.getUpdatedAt());
    }

    public ItemListResponse updatePurchased(UUID roomId, UUID itemId, UpdateItemPurchasedRequest request) {
        Item item = itemRepository.findByIdAndRoomId(itemId, roomId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        // StatusがACCEPTED以外のときはItemStatusInvalidExceptionをthrow
        if (!item.getStatus().equals(ItemStatus.ACCEPTED)) {
            throw new ItemStatusInvalidException(itemId, item.getStatus());
        }
        item.setPurchased(request.purchased());

        Item updatedItem = itemRepository.save(item);

        return new ItemListResponse(
                updatedItem.getId(),
                updatedItem.getRoom().getId(),
                updatedItem.getParticipant().getId(),
                updatedItem.getParticipant().getName(),
                updatedItem.getName(),
                updatedItem.getPrice(),
                updatedItem.getQuantity(),
                updatedItem.getMemo(),
                updatedItem.getStatus(),
                updatedItem.isPurchased(),
                updatedItem.getCreatedAt(),
                updatedItem.getUpdatedAt());
    }

    public ItemSummaryResponse getItemSummary(UUID roomId) {
        // ルームIDの存在チェック
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));

        List<Item> items = itemRepository.findByRoomId(roomId);

        List<Item> acceptedItems = items.stream().filter(item -> item.getStatus() == ItemStatus.ACCEPTED).toList();
        List<Item> proposedItems = items.stream().filter(item -> item.getStatus() == ItemStatus.PROPOSED).toList();
        List<Item> rejectedItems = items.stream().filter(item -> item.getStatus() == ItemStatus.REJECTED).toList();
        int acceptedItemCount = acceptedItems.size();

        ByStatus byStatus = new ByStatus(proposedItems.size(), acceptedItems.size(), rejectedItems.size());

        int acceptedTotalPrice = sumEstimatedPrice(acceptedItems);
        int proposedTotalPrice = sumEstimatedPrice(proposedItems);

        Integer remainingBudget;
        Integer overBudgetAmount;
        Integer budgetAmount;

        if (room.getBudgetAmount() != null) {
            budgetAmount = room.getBudgetAmount();
            remainingBudget = Math.max(0, budgetAmount - acceptedTotalPrice);
            overBudgetAmount = Math.max(0, acceptedTotalPrice - room.getBudgetAmount());
        } else {
            remainingBudget = null;
            overBudgetAmount = null;
            budgetAmount = null;
        }

        Map<UUID, List<Item>> itemsByParticipantId = items.stream()
                .collect(Collectors.groupingBy(item -> item.getParticipant().getId()));
        List<ParticipantSummary> perParticipant = itemsByParticipantId.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().get(0).getParticipant().getCreatedAt()))
                .map(entry -> toParticipantSummary(entry.getKey(), entry.getValue())).toList();

        List<Item> acceptedAndProposedItems = items.stream()
                .filter(item -> item.getStatus() == ItemStatus.ACCEPTED || item.getStatus() == ItemStatus.PROPOSED)
                .toList();
        Map<String, List<Item>> itemsByName = acceptedAndProposedItems.stream()
                .collect(Collectors.groupingBy(item -> item.getName()));
        List<ItemNameSummary> itemQuantities = itemsByName.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey()))
                .map(entry -> toItemNameSummary(entry.getKey(), entry.getValue())).toList();
        
        
            
        return new ItemSummaryResponse(roomId, acceptedTotalPrice, proposedTotalPrice, budgetAmount,
            remainingBudget, overBudgetAmount, acceptedItemCount, byStatus, perParticipant, itemQuantities);
    }

    /**
     * アイテムリストの値段の合計を求めるメソッド
     * 
     * @param items 合計の値段を求めたいアイテムのリスト
     * @return リストの各要素の値段 * 個数の合計値
     */
    private int sumEstimatedPrice(List<Item> items) {
        return items.stream().mapToInt(item -> item.getPrice() * item.getQuantity()).sum();
    }

    /**
     * 参加者IDと参加者の作成したアイテムリストを渡してParticipantSummaryを返すメソッド
     * 
     * @param participantId    参加者ID
     * @param participantItems そのIDの参加者が作ったアイテムのリスト
     * @return 参加者ID、 参加者の名前、アイテムのリストの長さ、アイテムリストの中で承認されたアイテムの価格の合計
     */
    private ParticipantSummary toParticipantSummary(UUID participantId, List<Item> participantItems) {
        String participantName = participantItems.get(0).getParticipant().getName();
        List<Item> acceptedItemsParticipant = participantItems.stream()
                .filter(item -> item.getStatus() == ItemStatus.ACCEPTED).toList();
        int proposalCount = participantItems.size();
        int acceptedTotalPrice = sumEstimatedPrice(acceptedItemsParticipant);
        return new ParticipantSummary(participantId, participantName, proposalCount, acceptedTotalPrice);
    }

    /**
     * nameとList<Item>をわたしてItemNameSummaryにして返すメソッド
     * 
     * @param name          Itemのname
     * @param sameNameItems Itemのstatusがrejectedのものを除いたItemリスト
     * @return ItemNameSummaryを返す
     */
    private ItemNameSummary toItemNameSummary(String name, List<Item> sameNameItems) {
        int totalQuantity = sameNameItems.stream().mapToInt(item -> item.getQuantity()).sum();
        int estimatedTotalPrice = sumEstimatedPrice(sameNameItems);
        return new ItemNameSummary(name, totalQuantity, estimatedTotalPrice);
    }

    public ItemListResponse updateItem(UUID roomId, UUID itemId, Operator operator, UpdateItemRequest request) {
        Item item = itemRepository.findByIdAndRoomId(itemId, roomId).orElseThrow(() -> new ItemNotFoundException(itemId));
        
        checkOperatorCanModify(item, operator);
        item.setName(request.name());
        item.setPrice(request.price());
        item.setQuantity(request.quantity());
        item.setMemo(request.memo());
        // 参加者による編集かつ status == ACCEPTEDの場合 status = PROPOSED , purchased = false に切り替える
        if (!operator.isHost() && item.getStatus() == ItemStatus.ACCEPTED) {
            item.setStatus(ItemStatus.PROPOSED);
            item.setPurchased(false);
        }

        Item updatedItem = itemRepository.save(item);
        return new ItemListResponse(
            updatedItem.getId(),
            updatedItem.getRoom().getId(),
            updatedItem.getParticipant().getId(),
            updatedItem.getParticipant().getName(), 
            updatedItem.getName(),
            updatedItem.getPrice(),
            updatedItem.getQuantity(),
            updatedItem.getMemo(),
            updatedItem.getStatus(),
            updatedItem.isPurchased(),
            updatedItem.getCreatedAt(),
            updatedItem.getUpdatedAt()
        );
    }
    
    public void deleteItem(UUID roomId, UUID itemId, Operator operator) {
        Item item = itemRepository.findByIdAndRoomId(itemId, roomId).orElseThrow(() -> new ItemNotFoundException(itemId));
        checkOperatorCanModify(item, operator);
        itemRepository.delete(item);
    }

    /**
     * 権限チェック(Roomのホスト or アイテム提案者の確認)
     * @param item アイテムオブジェクト
     * @param operator アイテム管理者
     */
    private void checkOperatorCanModify(Item item, Operator operator) {
        if (!operator.isHost() && !operator.participantId().equals(item.getParticipant().getId())) {
            throw new NotItemOwnerException(operator.participantId(), item.getId());
        }
    }


}