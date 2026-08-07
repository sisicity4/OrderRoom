package com.github.karuhito.orderroombackend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
import com.github.karuhito.orderroombackend.dto.CreateItemResponse;
import com.github.karuhito.orderroombackend.dto.ItemListResponse;
import com.github.karuhito.orderroombackend.dto.ItemNameSummary;
import com.github.karuhito.orderroombackend.dto.ItemSummaryResponse;
import com.github.karuhito.orderroombackend.dto.ParticipantSummary;
import com.github.karuhito.orderroombackend.dto.UpdateItemPurchasedRequest;
import com.github.karuhito.orderroombackend.dto.UpdateItemStatusRequest;

import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;

import com.github.karuhito.orderroombackend.exception.ItemNotFoundException;
import com.github.karuhito.orderroombackend.exception.ItemStatusInvalidException;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;

import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;


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
            savedItem.getUpdatedAt()
        );
    }

    public List<ItemListResponse> getItems(UUID roomId, ItemStatus status, UUID participantId) {
        roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
        List<Item> items = itemRepository.findItemsByRoomAndFilters(roomId, status, participantId);
        return items.stream().map(item -> new ItemListResponse(item.getId(), item.getRoom().getId(), item.getParticipant().getId(), item.getParticipant().getName(), item.getName(), item.getPrice(), item.getQuantity(), item.getMemo(), item.getStatus(), item.isPurchased(), item.getCreatedAt(), item.getUpdatedAt())).toList();
    }

    // ItemListResponseを再利用する
    public ItemListResponse updateStatus(UUID roomId, UUID itemId, UpdateItemStatusRequest request) {
        Item item = itemRepository.findByIdAndRoomId(itemId, roomId).orElseThrow(() -> new ItemNotFoundException(itemId));
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
            updatedItem.getUpdatedAt()
        );
    }

    public ItemListResponse updatePurchased(UUID roomId, UUID itemId, UpdateItemPurchasedRequest request) {
        Item item = itemRepository.findByIdAndRoomId(itemId, roomId).orElseThrow(() -> new ItemNotFoundException(itemId));

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
            updatedItem.getUpdatedAt()
        );
    }

    public ItemSummaryResponse getItemSummary(UUID roomId) {
        // ルームIDの存在チェック
        roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
        // ルーム内のアイテム一覧を取得
        List<Item> items = itemRepository.findByRoomId(roomId);

        // StatusがREJECTED以外のアイテムに絞り込む
        List<Item> filteredItems = items.stream().filter(item -> item.getStatus() != ItemStatus.REJECTED).toList();
        int totalPrice = filteredItems.stream().mapToInt(item -> item.getPrice() * item.getQuantity()).sum();

        // 参加者ごとのグループ化+合計金額
        Map<UUID, List<Item>> itemsByParticipantId = filteredItems.stream().collect(Collectors.groupingBy(item -> item.getParticipant().getId()));
        List<ParticipantSummary> participantSummaries = itemsByParticipantId.entrySet().stream().sorted(Comparator.comparing(entry ->  entry.getValue().get(0).getParticipant().getCreatedAt())).map(entry -> new ParticipantSummary(entry.getKey(), entry.getValue().get(0).getParticipant().getName(), entry.getValue().stream().mapToInt(item -> item.getPrice() * item.getQuantity()).sum())).toList();
        
        // 商品名ごとのグループ化+合計数量
        Map<String, List<Item>> itemsByName = filteredItems.stream().collect(Collectors.groupingBy(item -> item.getName()));
        List<ItemNameSummary> itemNameSummaries = itemsByName.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey())).map(entry -> new ItemNameSummary(entry.getKey(), entry.getValue().stream().mapToInt(item -> item.getQuantity()).sum())).toList();

        return new ItemSummaryResponse(totalPrice, participantSummaries, itemNameSummaries);
    }
}