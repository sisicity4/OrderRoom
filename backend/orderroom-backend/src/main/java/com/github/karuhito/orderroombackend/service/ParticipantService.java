package com.github.karuhito.orderroombackend.service;

import org.springframework.stereotype.Service;

import com.github.karuhito.orderroombackend.dto.CreateParticipantRequest;
import com.github.karuhito.orderroombackend.dto.CreateParticipantResponse;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;

import java.util.UUID;

@Service
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final RoomRepository roomRepository;

    public ParticipantService(ParticipantRepository participantRepository, RoomRepository roomRepository){
        this.participantRepository = participantRepository;
        this.roomRepository = roomRepository;
    }

    public CreateParticipantResponse createParticipant(UUID roomId, CreateParticipantRequest request) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId)); // 404の自作例外
        Participant participant = new Participant(room,request.name());
        Participant savedParticipant = participantRepository.save(participant);
        return new CreateParticipantResponse(
            savedParticipant.getId(),
            savedParticipant.getRoom().getId(),
            savedParticipant.getName(),
            savedParticipant.getToken(),
            savedParticipant.getCreatedAt()
        );
    }
}