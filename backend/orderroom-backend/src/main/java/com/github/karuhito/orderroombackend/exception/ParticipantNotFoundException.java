package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class ParticipantNotFoundException extends RuntimeException {
    public ParticipantNotFoundException(UUID participantId) {
        super(participantId.toString());
    }
}