package com.github.karuhito.orderroombackend.dto;

import java.util.UUID;

public record ParticipantSummary(
    UUID id,
    String name,
    int totalPrice
) {
}