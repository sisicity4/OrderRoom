package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;


public class InvalidHostKeyException extends RuntimeException {
    private InvalidHostKeyReason reason;

    public InvalidHostKeyException(UUID roomId, InvalidHostKeyReason reason) {
        super(roomId.toString());
        this.reason = reason;
    }

    public InvalidHostKeyReason getReason() {
        return reason;
    }

}
