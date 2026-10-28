package com.github.karuhito.orderroombackend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.karuhito.orderroombackend.entity.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByRoomId(UUID roomId);
    List<Settlement> findByRoomIdAndSettledAtIsNull(UUID roomId);
    void deleteByRoomIdAndSettledAtIsNull(UUID roomId);
    boolean existsByRoomIdAndSettledAtIsNotNull(UUID roomId);
    Optional<Settlement> findByIdAndRoomId(UUID id, UUID roomId);
}