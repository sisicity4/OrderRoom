package com.github.karuhito.orderroombackend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
import com.github.karuhito.orderroombackend.dto.CreateItemResponse;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.Participant;
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
}