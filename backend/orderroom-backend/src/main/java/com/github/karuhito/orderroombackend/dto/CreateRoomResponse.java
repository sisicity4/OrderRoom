package com.github.karuhito.orderroombackend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRoomResponse(
    UUID id,
    String title,
    LocalDate eventDate,
    String memo,
    Integer budgetAmount,
    UUID hostKey,
    String participantUrl,
    String hostUrl,
    Instant createdAt
) {
}