package com.github.karuhito.orderroombackend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
    @NotBlank(message = "空白は許可されていません")
    @Size(max = 100)
    String title,

    LocalDate eventDate,
    String memo
) {
}
