package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class NotSettlementPayerException extends RuntimeException {
    private final UUID settlementId;

    public NotSettlementPayerException(UUID participantId, UUID settlementId) {
        super(participantId.toString());
        this.settlementId = settlementId;
    }

    public UUID getSettlementId() {
        return settlementId;
    }
}