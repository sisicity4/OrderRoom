package com.github.karuhito.orderroombackend.dto;

import java.util.UUID;

public record HostParticipantResponse(
    UUID id,
    String name,
    UUID token
) {
}