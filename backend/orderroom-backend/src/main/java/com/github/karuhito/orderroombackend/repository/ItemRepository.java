package com.github.karuhito.orderroombackend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;

public interface ItemRepository  extends JpaRepository<Item, UUID>{
    @Query("SELECT i FROM Item i WHERE (i.room.id = :roomId) AND (:status IS NULL OR i.status = :status) AND (:participantId IS NULL OR i.participant.id = :participantId)")
    List<Item> findItemsByRoomAndFilters(@Param("roomId") UUID roomId, @Param("status") ItemStatus status, @Param("participantId") UUID participantId);
}
