package com.github.karuhito.orderroombackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.github.karuhito.orderroombackend.TestcontainersConfiguration;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

/* 商品一覧・集計・参加者一覧の GET に、参加者 token かホストキーの認証がかかることのテスト（#101） */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class GetEndpointsAuthTest {
    private static final String ITEMS_PATH = "/api/rooms/{roomId}/items";
    private static final String SUMMARY_PATH = "/api/rooms/{roomId}/summary";
    private static final String PARTICIPANTS_PATH = "/api/rooms/{roomId}/participants";

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired ParticipantRepository participants;

    private Room room;
    private Participant member;
    private Room otherRoom;
    private Participant outsider;

    @BeforeEach
    void setup() {
        room = rooms.save(new Room("GET認証"));
        member = participants.save(new Participant(room, "A"));
        otherRoom = rooms.save(new Room("別ルーム"));
        outsider = participants.save(new Participant(otherRoom, "C"));
    }

    // ---- 正常系 ----

    @ParameterizedTest
    @ValueSource(strings = {ITEMS_PATH, SUMMARY_PATH, PARTICIPANTS_PATH})
    void withParticipantTokenIsOk(String path) throws Exception {
        mvc.perform(get(path, room.getId())
                .header("X-Participant-Token", member.getToken().toString()))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {ITEMS_PATH, SUMMARY_PATH, PARTICIPANTS_PATH})
    void withHostKeyIsOk(String path) throws Exception {
        mvc.perform(get(path, room.getId())
                .header("X-Host-Key", room.getHostKey().toString()))
            .andExpect(status().isOk());
    }

    // ---- 認証 ----

    @ParameterizedTest
    @ValueSource(strings = {ITEMS_PATH, SUMMARY_PATH, PARTICIPANTS_PATH})
    void withoutHeadersIsForbidden(String path) throws Exception {
        mvc.perform(get(path, room.getId()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {ITEMS_PATH, SUMMARY_PATH, PARTICIPANTS_PATH})
    void withInvalidCredentialsIsForbidden(String path) throws Exception {
        // 存在しない token / UUID として不正な token
        for (String token : new String[] {UUID.randomUUID().toString(), "not-a-uuid"}) {
            mvc.perform(get(path, room.getId()).header("X-Participant-Token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }
        // 一致しないホストキー / UUID として不正なホストキー
        for (String hostKey : new String[] {UUID.randomUUID().toString(), "not-a-uuid"}) {
            mvc.perform(get(path, room.getId()).header("X-Host-Key", hostKey))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }
    }

    @ParameterizedTest // 別ルームの token / ホストキーは拒否する（設計書4.4）
    @ValueSource(strings = {ITEMS_PATH, SUMMARY_PATH, PARTICIPANTS_PATH})
    void withOtherRoomsCredentialsIsForbidden(String path) throws Exception {
        mvc.perform(get(path, room.getId())
                .header("X-Participant-Token", outsider.getToken().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(get(path, room.getId())
                .header("X-Host-Key", otherRoom.getHostKey().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ---- 異常系 ----

    @ParameterizedTest // ルームの存在は認証より先に確かめる
    @ValueSource(strings = {ITEMS_PATH, SUMMARY_PATH, PARTICIPANTS_PATH})
    void unknownRoomIsNotFound(String path) throws Exception {
        UUID unknownRoomId = UUID.randomUUID();

        mvc.perform(get(path, unknownRoomId)
                .header("X-Participant-Token", member.getToken().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
        mvc.perform(get(path, unknownRoomId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
    }
}
