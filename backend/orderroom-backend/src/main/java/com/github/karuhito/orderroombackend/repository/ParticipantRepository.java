package com.github.karuhito.orderroombackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import com.github.karuhito.orderroombackend.entity.Participant;

public interface ParticipantRepository  extends JpaRepository<Participant, UUID>{

}