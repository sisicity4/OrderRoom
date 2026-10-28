package com.github.karuhito.orderroombackend.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.exception.SettlementLockedException;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;

@Component
public class SettlementLockGuard {
    private final RoomRepository roomRepository;
    private final SettlementRepository settlementRepository;

    public SettlementLockGuard(RoomRepository roomRepository, SettlementRepository settlementRepository) {
        this.roomRepository = roomRepository;
        this.settlementRepository = settlementRepository;
    }

    public Room lockRoom(UUID roomId) {
        return roomRepository.findByIdForUpdate(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    public void ensureNotSettled(UUID roomId) {
        if (settlementRepository.existsByRoomIdAndSettledAtIsNotNull(roomId)) {
            throw new SettlementLockedException(roomId);
        }
    }
}