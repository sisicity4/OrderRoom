package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException(UUID roomId) {
        super(roomId.toString());
    }
}