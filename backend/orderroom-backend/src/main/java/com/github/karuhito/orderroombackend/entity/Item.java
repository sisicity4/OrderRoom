package com.github.karuhito.orderroombackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;


@Entity
@Table(name = "items")
public class Item {
    @Id
    @Generated(event = EventType.INSERT)
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
        nullable = false
    )
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "participant_id",
        nullable = false
    )
    private Participant participant;
    
    @Column(
        name = "name",
        length = 100,
        nullable = false
    )
    private String name;

    @Column(
        name = "price",
        nullable = false,
        columnDefinition = "integer default 0 check (price >= 0)"
    )
    private int price;

    @Column(
        name = "quantity",
        nullable = false,
        columnDefinition = "integer default 1 CHECK (quantity >= 1)"
    )
    private int quantity;

    @Column(name = "memo")
    private String memo;

    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "varchar(20) default 'proposed' check (status in ('proposed', 'accepted', 'rejected'))"
    )
    private ItemStatus status;

    @Column(
        name = "purchased",
        nullable = false,
        columnDefinition = "boolean default false"
    )
    private boolean purchased;

    @Generated(event = EventType.INSERT)
    @Column(
        name = "created_at",
        nullable = false,
        columnDefinition = "timestamptz default now()"
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    @Column(
        name = "updated_at",
        nullable = false,
        columnDefinition = "timestamptz default now()"
    )
    private Instant updatedAt;

    /**
     * コンストラクタ
     */

    public Item() {
        this.status = ItemStatus.PROPOSED;
        this.purchased = false;
    }

    public Item(Room room, Participant participant, String name) {
        this.room = room;
        this.participant = participant;
        this.name = name;
        this.status = ItemStatus.PROPOSED;
        this.purchased = false;
    }

    public Item(Room room, Participant participant, String name, int price, int quantity, String memo) {
        this.room = room;
        this.participant = participant;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.memo = memo;
        this.status = ItemStatus.PROPOSED;
        this.purchased = false;
    }

    /*  getter: id | room_id | participant_id | name | price | quantity | memo | status | created_at | updated_at
    isPurchased でTrue / Falseをチェック
    */

    public UUID getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public Participant getParticipant() {
        return participant;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getMemo() {
        return memo;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * setter
     * issue#34で対応: name | price | quantity | memo
     * issue#14で対応: status
     * issue#15で対応: purchased
    */

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }


    public void setStatus(ItemStatus status) {
        this.status = status;
    }


    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }
}
