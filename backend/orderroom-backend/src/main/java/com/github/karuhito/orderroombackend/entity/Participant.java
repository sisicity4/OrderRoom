package com.github.karuhito.orderroombackend.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/* 参加者テーブル  */
@Entity
@Table(name = "participants")
public class Participant {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        updatable = false,
        columnDefinition = "uuid DEFAULT gen_random_uuid()"
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "room_id",
        nullable = false
    )
    private Room room;

    @Column(
        name = "name",
        length = 50,
        nullable = false
    )
    private String name;

    /**
     * issue#33でtokenカラム追加予定(本人確認用の秘密値)
     * UUID | NOT NULL | UNIQUE | DEFAULT gen_random_uuid()
     */

    @Generated(event = EventType.INSERT)
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
        columnDefinition = "timestamptz DEFAULT now()"
    )
    private Instant createdAt;

    /* 引数無しコンストラクタ */
    public Participant() {

    }

    /* nameを持たせたコンストラクタ */
    public Participant(Room room, String name) {
        this.room = room;
        this.name = name;
    }

    //getter: id | room_id | name | created_at | 後にtokenも記述
    public UUID getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // setterなし: nameを編集するAPIは現状存在しないため、意図的に未実装(コンストラクタでのみ設定)
    
}