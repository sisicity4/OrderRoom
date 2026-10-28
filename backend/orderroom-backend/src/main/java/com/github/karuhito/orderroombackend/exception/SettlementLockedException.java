package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class SettlementLockedException extends RuntimeException {
    public SettlementLockedException(UUID roomId) {
        super(roomId.toString());
    }
}