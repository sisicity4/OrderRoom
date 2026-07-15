package com.github.karuhito.orderroombackend.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.karuhito.orderroombackend.dto.CreateRoomRequest;
import com.github.karuhito.orderroombackend.dto.CreateRoomResponse;
import com.github.karuhito.orderroombackend.entity.Room;
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

      Room savedRoom = roomRepository.save(room);

     
      // id,title,eventDate,memo,hostKey,participantUrl,hostUrl,createdAt
      String participantUrl = frontendBaseUrl + "/rooms/" + savedRoom.getId();
      String hostUrl = frontendBaseUrl + "/rooms/" + savedRoom.getId() + "/host?key=" + savedRoom.getHostKey();
      CreateRoomResponse roomResponse = new CreateRoomResponse(savedRoom.getId(), savedRoom.getTitle(), savedRoom.getEventDate(), savedRoom.getMemo(), savedRoom.getHostKey(), participantUrl, hostUrl, savedRoom.getCreatedAt());

      return roomResponse;
    }
}