package com.github.karuhito.orderroombackend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "settlement_members")
public class SettlementMember {
    @EmbeddedId
    private SettlementMemberId id;

    @MapsId("roomId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @MapsId("participantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "created_at", nullable = false, updatable = false,
        columnDefinition = "timestamp with time zone DEFAULT now()")
    private Instant createdAt;

    protected SettlementMember() {}

    public SettlementMember(Participant participant) {
        this.participant = participant;
        this.room = participant.getRoom();
        this.id = new SettlementMemberId(room.getId(), participant.getId());
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public SettlementMemberId getId() {
        return id;
    }

    public Participant getParticipant() {
        return participant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
