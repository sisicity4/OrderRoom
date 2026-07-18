package com.github.karuhito.orderroombackend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
import com.github.karuhito.orderroombackend.dto.CreateItemResponse;
import com.github.karuhito.orderroombackend.service.ItemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms/{roomId}/items")

public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }
    
    @PostMapping
    public ResponseEntity<CreateItemResponse> createItem(@PathVariable UUID roomId, @Valid @RequestBody CreateItemRequest request) {
        CreateItemResponse response = itemService.createItem(roomId, request);

        return ResponseEntity.status(201).body(response);
    }
}
