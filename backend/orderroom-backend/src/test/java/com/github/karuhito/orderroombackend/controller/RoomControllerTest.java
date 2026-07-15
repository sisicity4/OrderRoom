package com.github.karuhito.orderroombackend.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.github.karuhito.orderroombackend.dto.CreateRoomRequest;

import tools.jackson.databind.ObjectMapper;


@SpringBootTest
@AutoConfigureMockMvc

public class RoomControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // CreateRoom
    @Test
    void createRoomTest() throws Exception {
        // リクエストDTOを組み立てる
        CreateRoomRequest request = new CreateRoomRequest("testTitle", null, null);

        // mockMvcでPOSTリクエストを送る
        mockMvc.perform(
            post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        // 検証
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("testTitle"))
        .andExpect(jsonPath("$.hostKey").exists());
    }

    @Test
    void createRoomValidTest() throws Exception {
        // 全てnull
        CreateRoomRequest request = new CreateRoomRequest(null, null, null);

        mockMvc.perform(
            post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALID_ERROR"))
        .andExpect(jsonPath("$.message").value("不正な入力です"))
        .andExpect(jsonPath("$.fields.title").value("空白は許可されていません"));
    }
    
}
