package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

public class RoomNotFoundException extends RuntimeException{
    public RoomNotFoundException(UUID roomId) {
        super(roomId.toString());
    }
}