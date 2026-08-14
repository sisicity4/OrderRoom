package com.github.karuhito.orderroombackend.dto;

import java.util.UUID;
import java.util.List;

/**
 * アイテムの集計を返すレコード
 * ItemSummaryResponse
 * @param roomId ルームのID
 * @param acceptedTotalPrice statusがacceptedのアイテムの合計金額
 * @param proposedTotalPrice statusがproposedのアイテムの合計金額
 * @param budgetAmount ルームの予算上限 未設定ならnull 
 * @param remainingBudget 予算の残り 予算を未設定の場合null
 * @param overBudgetAmount 予算上限を超えた場合超えた分の数値を入れる。 超えない場合は0 予算未設定の場合はnull
 * @param acceptedItemCount statusがacceptedのアイテムの件数(個数ではない)
 * @param byStatus ステータス別の件数
 * @param perParticipant 参加者ごとの集計
 * @param itemQuantities 商品名ごとの集計
 */
public record ItemSummaryResponse(
    UUID roomId,
    int acceptedTotalPrice,
    int proposedTotalPrice,
    Integer budgetAmount,
    Integer remainingBudget,
    Integer overBudgetAmount,
    int acceptedItemCount,
    ByStatus byStatus,
    List<ParticipantSummary> perParticipant,
    List<ItemNameSummary> itemQuantities
) {
}