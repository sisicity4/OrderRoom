package com.github.karuhito.orderroombackend.dto;

/**
 * ステータスがrejectedではない(ステータスがacceptedまたはproposedになっている)アイテムの名前ごとの集計レコード
 * ItemNameSummary
 * @param name アイテム名 同名のアイテムをまとめたキー
 * @param totalQuantity その商品名の数量の合計
 * @param estimatedTotalPrice  その商品名の見積合計 = price * quantity
 */
public record ItemNameSummary(
    String name, 
    int totalQuantity,
    int estimatedTotalPrice
) {
}