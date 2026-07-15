package com.github.karuhito.orderroombackend.dto;

import java.util.Map;

public record ErrorResponse(
    String error,
    String message,
    Map<String, String> fields
) {
}