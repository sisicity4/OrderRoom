package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class NoSettlementMembersException extends RuntimeException {
    public NoSettlementMembersException(UUID roomId) {
        super(roomId.toString());
    }
}