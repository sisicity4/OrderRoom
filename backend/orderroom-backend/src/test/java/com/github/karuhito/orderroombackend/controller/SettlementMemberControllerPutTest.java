package com.github.karuhito.orderroombackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.github.karuhito.orderroombackend.TestcontainersConfiguration;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;
import com.github.karuhito.orderroombackend.service.SettlementMemberService;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class SettlementMemberControllerPutTest {
    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;
    @Autowired SettlementRepository settlements;
    @Autowired SettlementMemberService service;
    private Room room;
    private Participant a;
    private Participant b;

    @BeforeEach
    void setup() {
        room = rooms.save(new Room("精算対象"));
        a = participants.save(new Participant(room, "A"));
        b = participants.save(new Participant(room, "B"));
    }

    private MockHttpServletRequestBuilder request(String body) {
        return put("/api/rooms/{roomId}/settlement-members", room.getId())
            .header("X-Host-Key", room.getHostKey().toString())
            .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String body(UUID... ids) {
        return "{\"participantIds\":[" + java.util.Arrays.stream(ids)
            .map(id -> "\"" + id + "\"").collect(java.util.stream.Collectors.joining(",")) + "]}";
    }

    @Test
    void replacesAndReturnsSavedIdsInRequestOrder() throws Exception {
        mvc.perform(request(body(a.getId(), b.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantIds[0]").value(a.getId().toString()))
            .andExpect(jsonPath("$.participantIds[1]").value(b.getId().toString()));
        mvc.perform(request(body(b.getId()))).andExpect(status().isOk())
            .andExpect(jsonPath("$.participantIds.length()").value(1));
        mvc.perform(request(body(b.getId()))).andExpect(status().isOk());
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactly(b.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"participantIds\":null}", "{\"participantIds\":[]}",
        "{\"participantIds\":[null]}", "{\"participantIds\":[\"invalid\"]}"})
    void rejectsInvalidInput(String body) throws Exception {
        mvc.perform(request(body)).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicatesWithoutChangingSelection() throws Exception {
        mvc.perform(request(body(a.getId()))).andExpect(status().isOk());
        mvc.perform(request(body(b.getId(), b.getId()))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALID_ERROR"))
            .andExpect(jsonPath("$.fields.participantIds").value("参加者IDを重複して指定できません"))
            .andExpect(jsonPath("$.fields.participantIdsUnique").doesNotExist());
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactly(a.getId());
    }

    @Test
    void rejectsForeignAndMissingIdsIdenticallyWithoutPartialSave() throws Exception {
        Room other = rooms.save(new Room("別ルーム"));
        Participant outsider = participants.save(new Participant(other, "C"));
        mvc.perform(request(body(a.getId()))).andExpect(status().isOk());
        String foreignResponse = mvc.perform(request(body(b.getId(), outsider.getId())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("PARTICIPANT_NOT_FOUND"))
            .andReturn().getResponse().getContentAsString();
        String missingResponse = mvc.perform(request(body(b.getId(), UUID.randomUUID())))
            .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(missingResponse).isEqualTo(foreignResponse);
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactly(a.getId());
    }

    @Test
    void requiresHostKeyEvenWithParticipantToken() throws Exception {
        mvc.perform(put("/api/rooms/{roomId}/settlement-members", room.getId())
            .header("X-Participant-Token", a.getToken().toString())
            .contentType(MediaType.APPLICATION_JSON).content(body(a.getId())))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "00000000-0000-0000-0000-000000000000", ""})
    void rejectsInvalidHostKey(String key) throws Exception {
        mvc.perform(put("/api/rooms/{roomId}/settlement-members", room.getId())
            .header("X-Host-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(body(a.getId())))
            .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMissingRoom() throws Exception {
        mvc.perform(put("/api/rooms/{roomId}/settlement-members", UUID.randomUUID())
            .header("X-Host-Key", room.getHostKey().toString())
            .contentType(MediaType.APPLICATION_JSON).content(body(a.getId())))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
    }

    @Test
    void rejectsWithConflictWhenAnySettled() throws Exception {
        Settlement settled = new Settlement(room, b, a, 1000);
        settled.markSettled(Instant.parse("2026-02-01T00:00:00Z"));
        settlements.save(settled);

        mvc.perform(request(body(a.getId())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value("精算済みのため、変更できません"));
        // 対象者は未設定（全員）のまま
        assertThat(service.getEffectiveMembers(room.getId()))
            .extracting(Participant::getId).containsExactlyInAnyOrder(a.getId(), b.getId());
    }
}
