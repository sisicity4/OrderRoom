package com.github.karuhito.orderroombackend.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateItemRequest(
    @NotNull(message = "参加者を正しく指定してください")
    UUID participantId,

    @NotBlank(message = "アイテム名を正しく入力してください")
    @Size(max = 100, message = "アイテム名は100字以内で入力してください")
    String name,

    @NotNull(message = "値段を正しく入力してください")
    @Min(value = 0, message = "値段をマイナスに設定することはできません")
    Integer price,

    @NotNull(message = "個数を正しく入力してください")
    @Min(value = 1, message = "個数を0以下に設定することはできません")
    Integer quantity,

    @Size(max = 255, message = "メモは255字以内で入力してください")
    String memo
) {
}