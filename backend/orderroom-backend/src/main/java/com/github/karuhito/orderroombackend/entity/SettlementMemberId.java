package com.github.karuhito.orderroombackend.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SettlementMemberId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    protected SettlementMemberId() {}

    public SettlementMemberId(UUID roomId, UUID participantId) {
        this.roomId = Objects.requireNonNull(roomId);
        this.participantId = Objects.requireNonNull(participantId);
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SettlementMemberId that)) return false;
        return Objects.equals(roomId, that.roomId)
            && Objects.equals(participantId, that.participantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, participantId);
    }
}
