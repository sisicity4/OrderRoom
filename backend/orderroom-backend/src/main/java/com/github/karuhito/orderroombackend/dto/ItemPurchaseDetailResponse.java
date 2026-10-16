package com.github.karuhito.orderroombackend.dto;

import java.util.UUID;

public record ItemPurchaseDetailResponse(
    UUID id,
    String name,
    Integer actualPrice,
    UUID paidByParticipantId,
    String paidByParticipantName
) {
}