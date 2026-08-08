package com.github.karuhito.orderroombackend.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.karuhito.orderroombackend.dto.CreateRoomRequest;
import com.github.karuhito.orderroombackend.dto.CreateRoomResponse;
import com.github.karuhito.orderroombackend.dto.RoomResponse;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

@Service
public class RoomService  {
    private final RoomRepository roomRepository;
    private final String frontendBaseUrl;
    
    // コンストラクタインジェクション
    public RoomService(RoomRepository roomRepository,     @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.roomRepository = roomRepository;
        this.frontendBaseUrl = frontendBaseUrl;

    }


    public CreateRoomResponse createRoom(CreateRoomRequest request){
      Room room = new Room(request.title());
      room.setEventDate(request.eventDate());
      room.setMemo(request.memo());
      room.setBudgetAmount(request.budgetAmount());

      Room savedRoom = roomRepository.save(room);

     
      // id, title, eventDate, memo, budgetAmount, hostKey, participantUrl, hostUrl, createdAt
      String participantUrl = frontendBaseUrl + "/rooms/" + savedRoom.getId();
      String hostUrl = frontendBaseUrl + "/rooms/" + savedRoom.getId() + "/host?key=" + savedRoom.getHostKey();
      CreateRoomResponse roomResponse = new CreateRoomResponse(savedRoom.getId(), savedRoom.getTitle(), savedRoom.getEventDate(), savedRoom.getMemo(), savedRoom.getBudgetAmount(), savedRoom.getHostKey(), participantUrl, hostUrl, savedRoom.getCreatedAt());

      return roomResponse;
    }

    public RoomResponse getRoom(UUID roomId) {
      Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
      
      return new RoomResponse(
        room.getId(),
        room.getTitle(),
        room.getEventDate(),
        room.getMemo(),
        room.getBudgetAmount(),
        room.getCreatedAt()
      );
    }
}