package com.github.karuhito.orderroombackend.dto;

import java.util.UUID;

import com.github.karuhito.orderroombackend.validation.BothOrNeitherPresent;

import jakarta.validation.constraints.Min;

@BothOrNeitherPresent
public record UpdateItemPurchaseDetailRequest(
    @Min(
        value = 0,
        message = "実購入額をマイナスに設定することはできません"
    )
    Integer actualPrice,

    UUID paidByParticipantId
) {
}