package com.github.karuhito.orderroombackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;

import java.util.UUID;
import java.time.LocalDate;
import java.time.Instant;



/* テーブルのカラムを記述 */
@Entity
@Table(name = "rooms")
public class Room {
    @Id
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

    @Column(
        name = "budget_amount",
        check = @CheckConstraint(
            name = "rooms_budget_amount_check",
            constraint = "budget_amount >= 0"
        )
    )
    private Integer budgetAmount;

    @Column(
        name = "host_key",
        nullable = false,
        unique = true,
        updatable = false,columnDefinition = "uuid DEFAULT gen_random_uuid()"
    )
    private UUID hostKey;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
        columnDefinition = "timestamp with time zone DEFAULT now()"
    )
    private Instant createdAt;
    /**
     * 引数無しコンストラクタ
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.hostKey == null) {
            this.hostKey = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
    }

    public Room() {

    }

    /**
     * 最低限(titleのみを受け取るとき)のコンストラクタ
     */
    public Room(String title ) {
        this.title = title;
        
    }


    // getter: id | title | eventDate | memo | budgetAmount | hostKey | createdAt
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

    public Integer getBudgetAmount() {
        return budgetAmount;
    }

    public UUID getHostKey() {
        return hostKey;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }

    // setter: title | eventDate | memo | budgetAmount
    public void setTitle(String title) {
        this.title = title;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
    
    public void setBudgetAmount(Integer budgetAmount) {
        this.budgetAmount = budgetAmount;
    }
}
