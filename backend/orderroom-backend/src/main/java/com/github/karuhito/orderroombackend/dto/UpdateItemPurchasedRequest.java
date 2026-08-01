package com.github.karuhito.orderroombackend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateItemPurchasedRequest(
    @NotNull(message = "purchasedを正しく入力してください")
    Boolean purchased
) {
}