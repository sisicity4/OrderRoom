package com.github.karuhito.orderroombackend.controller;

import com.github.karuhito.orderroombackend.dto.UpdateItemStatusRequest;
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
import com.github.karuhito.orderroombackend.dto.UpdateItemPurchasedRequest;
import com.github.karuhito.orderroombackend.dto.UpdateItemRequest;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.resolver.CurrentOperator;
import com.github.karuhito.orderroombackend.resolver.CurrentParticipant;
import com.github.karuhito.orderroombackend.resolver.Operator;
import com.github.karuhito.orderroombackend.service.ItemService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/rooms/{roomId}/items")

public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }
    
    @PostMapping // アイテム作成
    public ResponseEntity<CreateItemResponse> createItem(@CurrentParticipant Participant participant, @Valid @RequestBody CreateItemRequest request) {
        CreateItemResponse response = itemService.createItem(participant, request);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping // ルームのアイテムを一覧で取得
    public ResponseEntity<List<ItemListResponse>> getItems(@PathVariable UUID roomId, @RequestParam(required = false)ItemStatus status, @RequestParam(required = false)UUID participantId) {
        List<ItemListResponse> response = itemService.getItems(roomId, status, participantId);
        return ResponseEntity.status(200).body(response);
    }
       
    @PatchMapping("/{itemId}/status") // アイテムのStatusを更新する
    public ResponseEntity<ItemListResponse> updateStatus(@PathVariable UUID roomId, @PathVariable UUID itemId, @Valid @RequestBody UpdateItemStatusRequest request ) {
        ItemListResponse response = itemService.updateStatus(roomId, itemId, request);
        return ResponseEntity.status(200).body(response);
    }

    @PatchMapping("/{itemId}/purchased") // アイテムのpurchasedを更新する
    public ResponseEntity<ItemListResponse> updatePurchased(@PathVariable UUID roomId, @PathVariable UUID itemId, @Valid @RequestBody UpdateItemPurchasedRequest request) {
        ItemListResponse response = itemService.updatePurchased(roomId, itemId, request);
        return ResponseEntity.status(200).body(response);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemListResponse> updateItem(@PathVariable UUID roomId, @PathVariable UUID itemId, @CurrentOperator Operator operator, @Valid @RequestBody UpdateItemRequest request) {
        ItemListResponse response = itemService.updateItem(roomId, itemId, operator, request);
        return ResponseEntity.status(200).body(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID roomId, @PathVariable UUID itemId, @CurrentOperator Operator operator) {
        itemService.deleteItem(roomId, itemId, operator);
        return ResponseEntity.status(204).build();
        
    }
}