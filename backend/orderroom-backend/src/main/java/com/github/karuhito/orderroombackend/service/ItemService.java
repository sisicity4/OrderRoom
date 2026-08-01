package com.github.karuhito.orderroombackend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
import com.github.karuhito.orderroombackend.dto.CreateItemResponse;
import com.github.karuhito.orderroombackend.dto.ItemListResponse;
import com.github.karuhito.orderroombackend.dto.UpdateItemPurchasedRequest;
import com.github.karuhito.orderroombackend.dto.UpdateItemStatusRequest;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.exception.ItemNotFoundException;
import com.github.karuhito.orderroombackend.exception.ItemStatusInvalidException;
import com.github.karuhito.orderroombackend.exception.ParticipantNotFoundException;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

@Service

public class ItemService {
    private final ItemRepository itemRepository;
    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;

    public ItemService(ItemRepository itemRepository, RoomRepository roomRepository,ParticipantRepository participantRepository ) {
        this.itemRepository = itemRepository;
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
    }

    public CreateItemResponse createItem(UUID roomId, CreateItemRequest request) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
        Participant participant = participantRepository.findByIdAndRoomId(request.participantId(), roomId).orElseThrow(() -> new ParticipantNotFoundException(request.participantId()));
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
}