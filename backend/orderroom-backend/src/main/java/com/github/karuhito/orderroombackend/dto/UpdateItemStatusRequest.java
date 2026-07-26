package com.github.karuhito.orderroombackend.dto;

import com.github.karuhito.orderroombackend.entity.ItemStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateItemStatusRequest(
    @NotNull
    ItemStatus status
) {
}