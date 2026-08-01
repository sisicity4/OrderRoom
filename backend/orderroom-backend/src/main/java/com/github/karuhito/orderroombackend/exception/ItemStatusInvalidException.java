package com.github.karuhito.orderroombackend.exception;

import java.util.UUID;

import com.github.karuhito.orderroombackend.entity.ItemStatus;

public class ItemStatusInvalidException extends RuntimeException{
    private ItemStatus status;
    public ItemStatusInvalidException(UUID itemId, ItemStatus status)  {
        super(itemId.toString());
        this.status = status;
    }

    public ItemStatus getStatus() {
        return status;
    }
    
}