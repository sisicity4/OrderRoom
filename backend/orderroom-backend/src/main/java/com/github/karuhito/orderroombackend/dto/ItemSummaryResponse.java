package com.github.karuhito.orderroombackend.dto;

import java.util.List;

public record ItemSummaryResponse(
    int totalPrice,
    List<ParticipantSummary> participantSummaries,
    List<ItemNameSummary> itemNameSummaries
) {
}