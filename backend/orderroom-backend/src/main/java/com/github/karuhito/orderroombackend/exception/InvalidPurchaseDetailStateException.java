package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class InvalidPurchaseDetailStateException extends RuntimeException { 
    private final InvalidPurchaseDetailStateReason reason;

    public InvalidPurchaseDetailStateException(UUID itemId, InvalidPurchaseDetailStateReason reason) {
        super(itemId.toString());
        this.reason = reason;
    }

    public InvalidPurchaseDetailStateReason getReason() {
        return reason;
    }
}