package com.github.karuhito.orderroombackend.exception;

import com.github.karuhito.orderroombackend.entity.ItemStatus;
import java.util.UUID;

public class ItemStatusInvalidException extends RuntimeException {
  private ItemStatus status;

  public ItemStatusInvalidException(UUID itemId, ItemStatus status) {
    super(itemId.toString());
    this.status = status;
  }

  public ItemStatus getStatus() {
    return status;
  }
}
