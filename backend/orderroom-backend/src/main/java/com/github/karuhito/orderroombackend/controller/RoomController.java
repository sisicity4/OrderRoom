package com.github.karuhito.orderroombackend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.karuhito.orderroombackend.dto.CreateRoomRequest;
import com.github.karuhito.orderroombackend.dto.CreateRoomResponse;
import com.github.karuhito.orderroombackend.dto.ItemSummaryResponse;
import com.github.karuhito.orderroombackend.service.ItemService;
import com.github.karuhito.orderroombackend.service.RoomService;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;




@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService roomService;
    private final ItemService itemService;

    public RoomController(RoomService roomService, ItemService itemService) {
        this.roomService = roomService;
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        CreateRoomResponse response = roomService.createRoom(request);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{roomId}/summary")
    public ResponseEntity<ItemSummaryResponse> getItemSummary(@PathVariable UUID roomId) {
        ItemSummaryResponse response = itemService.getItemSummary(roomId);
        return ResponseEntity.status(200).body(response);
    }
    
    
}