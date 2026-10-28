package com.github.karuhito.orderroombackend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SettlementResponse(
    int actualTotalPrice,
    List<IncompletePurchaseItem> incompletePurchaseItems,
    List<MemberSummary> members,
    List<TransferSummary> transfers,
    boolean locked
) {
    public record IncompletePurchaseItem(UUID id, String name) {}
    public record MemberSummary(UUID participantId, String name, int burdenAmount, int paidAmount, int balanceAmount) {}
    public record TransferSummary(UUID id, UUID fromParticipantId, UUID toParticipantId, int amount, Instant settledAt) {}
}