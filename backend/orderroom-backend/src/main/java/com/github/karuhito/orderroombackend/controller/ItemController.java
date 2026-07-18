package com.github.karuhito.orderroombackend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
import com.github.karuhito.orderroombackend.dto.CreateItemResponse;
import com.github.karuhito.orderroombackend.dto.ItemListResponse;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.service.ItemService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/rooms/{roomId}/items")

public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }
    
    @PostMapping // アイテム作成
    public ResponseEntity<CreateItemResponse> createItem(@PathVariable UUID roomId, @Valid @RequestBody CreateItemRequest request) {
        CreateItemResponse response = itemService.createItem(roomId, request);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping // ルームのアイテムを一覧で取得
    public ResponseEntity<List<ItemListResponse>> getItems(@PathVariable UUID roomId, @RequestParam(required = false)ItemStatus status, @RequestParam(required = false)UUID participantId) {
        List<ItemListResponse> response = itemService.getItems(roomId, status, participantId);
        return ResponseEntity.status(200).body(response);
    }
    
}
