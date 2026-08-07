package com.github.karuhito.orderroombackend.dto;

import java.time.Instant;
import java.util.UUID;

public record ParticipantListResponse(
    UUID id,
    String name,
    Instant createdAt
) {
}