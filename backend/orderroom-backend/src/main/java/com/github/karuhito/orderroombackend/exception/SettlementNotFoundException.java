package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class SettlementNotFoundException extends RuntimeException {
    public SettlementNotFoundException(UUID settlementId) {
        super(settlementId.toString());
    }    
}
