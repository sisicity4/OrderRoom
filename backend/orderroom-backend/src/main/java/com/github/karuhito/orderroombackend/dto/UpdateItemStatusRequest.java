package com.github.karuhito.orderroombackend.dto;

import com.github.karuhito.orderroombackend.entity.ItemStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateItemStatusRequest(
    @NotNull(message = "statusを正しく入力してください")
    ItemStatus status
) {
}