package com.github.karuhito.orderroombackend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.karuhito.orderroombackend.dto.SettlementResponse;
import com.github.karuhito.orderroombackend.dto.UpdateSettlementSettledRequest;
import com.github.karuhito.orderroombackend.resolver.CurrentOperator;
import com.github.karuhito.orderroombackend.resolver.Operator;
import com.github.karuhito.orderroombackend.service.SettlementService;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/rooms")
public class SettlementController {
    private final SettlementService settlementService;
    

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }


    @GetMapping("/{roomId}/settlement")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable UUID roomId, @CurrentOperator Operator operator) {
        SettlementResponse response = settlementService.getSettlement(roomId);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/{roomId}/settlement/recalculate")
    public ResponseEntity<SettlementResponse> postSettlementRecalculate(@PathVariable UUID roomId) {
        SettlementResponse response = settlementService.recalculate(roomId);
        
        return ResponseEntity.status(200).body(response);
    }
    
    @PostMapping("/{roomId}/settlement/reset")
    public ResponseEntity<SettlementResponse> postSettlementReset(@PathVariable UUID roomId) {
        SettlementResponse response = settlementService.resetSettled(roomId);

        return ResponseEntity.status(200).body(response);
    }

    @PatchMapping("/{roomId}/settlements/{settlementId}/settled")
    public ResponseEntity<SettlementResponse> patchSettlementSettled(@PathVariable UUID roomId, @PathVariable UUID settlementId, @CurrentOperator Operator operator, @Valid @RequestBody UpdateSettlementSettledRequest request) {
        SettlementResponse response = settlementService.updateSettled(roomId, settlementId, operator, request.settled());

        return ResponseEntity.status(200).body(response);
    }
}