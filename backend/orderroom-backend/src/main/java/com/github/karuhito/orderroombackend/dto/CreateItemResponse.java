package com.github.karuhito.orderroombackend.dto;

import com.github.karuhito.orderroombackend.entity.ItemStatus;
import java.time.Instant;
import java.util.UUID;

public record CreateItemResponse(
    UUID id,
    UUID roomId,
    UUID participantId,
    String name,
    int price,
    int quantity,
    String memo,
    ItemStatus status,
    boolean purchased,
    Instant createdAt,
    Instant updatedAt) {}
