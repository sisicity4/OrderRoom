package com.github.karuhito.orderroombackend.dto;

import java.time.Instant;
import java.util.UUID;


public record CreateParticipantResponse(
    UUID id,
    UUID roomId,
    String name,
    UUID token,
    Instant createdAt
) {
}