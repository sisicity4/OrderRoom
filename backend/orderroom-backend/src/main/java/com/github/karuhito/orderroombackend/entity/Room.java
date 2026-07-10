package com.github.karuhito.orderroombackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

import java.util.UUID;
import java.time.LocalDate;
import java.time.Instant;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/* テーブルのカラムを記述 */
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        columnDefinition = "uuid DEFAULT gen_random_uuid()"
    )
    private UUID id;

    @Column(
        name = "title",
        length = 100,
        nullable = false
    )
    private String title;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "memo")
    private String memo;

    @Generated(event = EventType.INSERT)
    @Column(
        name = "host_key",
        nullable = false,
        unique = true,
        updatable = false,columnDefinition = "uuid DEFAULT gen_random_uuid()"
    )
    private UUID hostKey;

    @Generated(event = EventType.INSERT)
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
        columnDefinition = "timestamptz DEFAULT now()"
    )
    private Instant createdAt;
    /**
     * 引数無しコンストラクタ
     */
    public Room() {

    }

    /**
     * 最低限(titleのみを受け取るとき)のコンストラクタ
     */
    public Room(String title ) {
        this.title = title;
        
    }


    // getter: id | title | eventDate | memo | hostKey | createdAt
    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public String getMemo() {
        return memo;
    }

    public UUID getHostKey() {
        return hostKey;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }

    // setter: title | eventDate | memo

    public void setTitle(String title) {
        this.title = title;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
