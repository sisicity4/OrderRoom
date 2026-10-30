package com.github.karuhito.orderroombackend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSettlementSettledRequest(
    @NotNull(message = "settledを正しく入力してください")
    Boolean settled
) {
}