package com.github.karuhito.orderroombackend.dto;

import java.util.UUID;

/**
 * 参加者ごとの集計レコード
 * ParticipantSummary
 * @param participantId 参加者ID
 * @param name 参加者の名前
 * @param proposalCount その参加者が提案したすべてのアイテムの件数
 * @param acceptedTotalPrice statusがacceptedのアイテムの合計金額
 */
public record ParticipantSummary(
    UUID participantId,
    String name,
    int proposalCount,
    int acceptedTotalPrice
) {
}