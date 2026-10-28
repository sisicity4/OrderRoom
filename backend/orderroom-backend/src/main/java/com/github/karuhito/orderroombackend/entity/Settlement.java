package com.github.karuhito.orderroombackend.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        columnDefinition = "uuid DEFAULT gen_random_uuid()"
    )
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "room_id",
        nullable = false,
        columnDefinition = "uuid"
    )
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "from_participant_id",
        nullable = false,
        columnDefinition = "uuid"
    )
    private Participant fromParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "to_participant_id",
        nullable = false,
        columnDefinition = "uuid"
    )
    private Participant toParticipant;

    @Column(
        name = "amount",
        nullable = false,
        check = @CheckConstraint(
            name = "settlements_amount_check",
            constraint = "amount >= 1"
        )
    )
    private int amount;

    @Column(
        name = "settled_at",
        columnDefinition = "timestamp with time zone"
    )
    private Instant settledAt;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
        columnDefinition = "timestamp with time zone default now()"
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
    }

    public Settlement() {
    }

    public Settlement(Room room, Participant fromParticipant, Participant toParticipant, int amount) {
        this.room = room;
        this.fromParticipant = fromParticipant;
        this.toParticipant = toParticipant;
        this.amount = amount;
    }


    public UUID getId() {
        return id;
    }


    public Room getRoom() {
        return room;
    }


    public Participant getFromParticipant() {
        return fromParticipant;
    }


    public Participant getToParticipant() {
        return toParticipant;
    }


    public int getAmount() {
        return amount;
    }


    public Instant getSettledAt() {
        return settledAt;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }   
}