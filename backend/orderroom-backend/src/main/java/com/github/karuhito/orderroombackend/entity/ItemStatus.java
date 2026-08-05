package com.github.karuhito.orderroombackend.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ItemStatus {
    PROPOSED("proposed"),
    ACCEPTED("accepted"),
    REJECTED("rejected");

    private final String value;

    ItemStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
