package com.github.karuhito.orderroombackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.github.karuhito.orderroombackend.entity.Participant;

public interface ParticipantRepository  extends JpaRepository<Participant, UUID>{
    Optional<Participant> findByIdAndRoomId(UUID id, UUID roomId);
    List<Participant> findByRoomIdOrderByCreatedAtAsc(UUID roomId);
    Optional<Participant> findByTokenAndRoomId(UUID token, UUID roomId);
}