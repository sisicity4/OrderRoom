package com.github.karuhito.orderroombackend.resolver;

import java.util.UUID;

public record Operator(
    OperatorType type,
    UUID participantId
) {
    public boolean isHost() {
        return type == OperatorType.HOST;
    }
}