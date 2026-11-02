package com.github.karuhito.orderroombackend.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/*
 * Spring が投げるクライアント起因の例外が、500 ではなく正しい 4xx になることを確認する（#120）
 * 例外はコントローラーより手前で起きるので、ハンドラを直接呼ばず、MockMvc で実際にリクエストを送る
 */
@SpringBootTest
@AutoConfigureMockMvc
public class FrameworkClientErrorTest {
    @Autowired
    private MockMvc mockMvc;

    // 1. 定義されていない HTTP メソッドは 405 METHOD_NOT_ALLOWED
    @Test // 1.1 POST 専用の /api/rooms を PUT で呼ぶ
    void putToPostOnlyPathReturns405() throws Exception {
        mockMvc.perform(put("/api/rooms"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("このHTTPメソッドは使えません"))
                .andExpect(jsonPath("$.fields").value(nullValue()));
    }

    @Test // 1.2 GET 専用の /api/health を DELETE で呼ぶ
    void deleteToGetOnlyPathReturns405() throws Exception {
        mockMvc.perform(delete("/api/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
    }

    // 2. 存在しないパスは 404 NOT_FOUND
    @Test // 2.1 /api 直下の存在しないパス
    void unknownPathReturns404() throws Exception {
        mockMvc.perform(get("/api/not-exists"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("APIが見つかりません"))
                .andExpect(jsonPath("$.fields").value(nullValue()));
    }

    @Test // 2.2 ルームの下の存在しないパス（#120 のコメントで追記した再現）
    void unknownPathUnderRoomReturns404() throws Exception {
        mockMvc.perform(post("/api/rooms/{roomId}/no-such-path", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // 3. 想定外の Content-Type は 415 UNSUPPORTED_MEDIA_TYPE
    @Test // 3.1 JSON を受け取る /api/rooms に text/plain で送る
    void textPlainToJsonPathReturns415() throws Exception {
        mockMvc.perform(
                post("/api/rooms")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("title=test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("Content-Typeが不正です"))
                .andExpect(jsonPath("$.fields").value(nullValue()));
    }
}
