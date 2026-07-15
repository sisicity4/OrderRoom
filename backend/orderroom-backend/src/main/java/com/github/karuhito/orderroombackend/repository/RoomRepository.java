package com.github.karuhito.orderroombackend.repository;

import com.github.karuhito.orderroombackend.entity.Room;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


/**
 * RoomRepository
 */
public interface RoomRepository extends JpaRepository<Room, UUID>{

    
}