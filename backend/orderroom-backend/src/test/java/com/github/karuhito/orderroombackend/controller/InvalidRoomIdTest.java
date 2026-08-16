package com.github.karuhito.orderroombackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.karuhito.orderroombackend.dto.CreateItemRequest;
import com.github.karuhito.orderroombackend.dto.UpdateItemRequest;
import com.github.karuhito.orderroombackend.dto.UpdateItemStatusRequest;
import com.github.karuhito.orderroombackend.entity.ItemStatus;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class InvalidRoomIdTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 1. 不正roomIdを400 TYPE_MISMATCH にする
    @Test // 1.1 interceptor経由
    void hostKeyInterceptorInvalidFormatRoomId() throws Exception {
        // 不正なroomIdと適当なitemIdを作成
        String roomId = "not-a-uuid";
        UUID itemId = UUID.randomUUID();
        UpdateItemStatusRequest request = new UpdateItemStatusRequest(ItemStatus.ACCEPTED);
        mockMvc.perform(
                patch("/api/rooms/{roomId}/items/{itemId}/status", roomId, itemId)
                        .header("X-Host-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
                .andExpect(jsonPath("$.fields.roomId").value("不正な値: not-a-uuid"));
    }

    @Test // ParticipantResolver経由
    void participantResolverInvalidFormatRoomId() throws Exception {
        // 不正なroomIdを用意
        String roomId = "not-a-uuid";
        CreateItemRequest request = new CreateItemRequest("テストアイテム", 100, 1, null);
        mockMvc.perform(
                post("/api/rooms/{roomId}/items", roomId)
                        .header("X-Participant-Token", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
                .andExpect(jsonPath("$.fields.roomId").value("不正な値: not-a-uuid"));
    }

    @Test // OperatorResolver経由
    void operatorResolverInvalidFormatRoomId() throws Exception {
        // 不正なroomIdと適当なitemIdを作成
        String roomId = "not-a-uuid";
        UUID itemId = UUID.randomUUID();
        UpdateItemRequest request = new UpdateItemRequest("テストアイテム", 100, 1, null);
        mockMvc.perform(
                patch("/api/rooms/{roomId}/items/{itemId}", roomId, itemId)
                        .header("X-Host-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value("パラメータの型が不正です"))
                .andExpect(jsonPath("$.fields.roomId").value("不正な値: not-a-uuid"));
    }
}