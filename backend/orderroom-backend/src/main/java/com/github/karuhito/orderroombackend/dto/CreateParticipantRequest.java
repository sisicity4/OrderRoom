package com.github.karuhito.orderroombackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateParticipantRequest(
    @NotBlank(message = "名前を正しく入力してください")
    @Size(max = 50, message = "名前は50字以内で入力してください")
    String name
) {
}