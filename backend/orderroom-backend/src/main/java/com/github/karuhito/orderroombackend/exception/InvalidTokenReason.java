
package com.github.karuhito.orderroombackend.exception;

public enum InvalidTokenReason {
    MISSING, // 欠如している
    INVALID_FORMAT, // 形式が違う
    MISMATCH // 一致しない・存在しない
}