package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class NotItemOwnerException extends RuntimeException {
    private final UUID itemId;
    public NotItemOwnerException(UUID participantId, UUID itemId) {
        super(participantId.toString());
        this.itemId = itemId;
    }

    public UUID getItemId() {
        return itemId;
    }
}
