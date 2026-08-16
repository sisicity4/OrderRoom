package com.github.karuhito.orderroombackend.util;

import java.util.UUID;

import com.github.karuhito.orderroombackend.exception.InvalidPathVariableException;

public final class RoomIdParser {
    private RoomIdParser() {}
    /**
     * roomIdを文字列で受け取り、UUIDに変換するメソッド。
     * 変換失敗時にはInvalidPathVariableExceptionが投げられる。
     * @param roomIdString roomIdの文字列。この時点では型はUUIDではなくString。
     * @return roomIdをUUIDにして返している
     */
    public static UUID roomIdParse(String roomIdString) {
        try {
            UUID roomId = UUID.fromString(roomIdString);
            return roomId;
        } catch (IllegalArgumentException e) {
            throw new InvalidPathVariableException("roomId", roomIdString);
        }
    }
}