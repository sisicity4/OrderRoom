package com.github.karuhito.orderroombackend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.karuhito.orderroombackend.dto.CreateParticipantRequest;
import com.github.karuhito.orderroombackend.dto.CreateParticipantResponse;
import com.github.karuhito.orderroombackend.service.ParticipantService;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/rooms/{roomId}/participants")
public class ParticipantController {
    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping
    public ResponseEntity<CreateParticipantResponse> createParticipant(@PathVariable UUID roomId, @Valid @RequestBody CreateParticipantRequest request) {
        CreateParticipantResponse response = participantService.createParticipant(roomId, request);

        return ResponseEntity.status(201).body(response);
    }
}