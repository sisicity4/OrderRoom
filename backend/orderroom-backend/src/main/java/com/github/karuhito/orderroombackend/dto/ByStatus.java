package com.github.karuhito.orderroombackend.dto;

public record ByStatus(
    int proposed,
    int accepted,
    int rejected
) {
}