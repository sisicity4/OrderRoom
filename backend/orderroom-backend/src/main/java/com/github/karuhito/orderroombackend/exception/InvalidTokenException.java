package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class InvalidTokenException extends RuntimeException {
    private final InvalidTokenReason reason;

    public InvalidTokenException(UUID roomId, InvalidTokenReason reason) {
        super(roomId.toString());
        this.reason = reason;
    }

    public InvalidTokenReason getReason() {
        return reason;
    }
}