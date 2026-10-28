package com.github.karuhito.orderroombackend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SettlementCalculator {
    /**
     * 計算の入力となる参加者。ルームの全参加者を渡す。
     *
     * @param participantId 参加者のID
     * @param name          参加者の名前
     * @param createdAt     参加登録の日時。余りの配分と並び順の基準になる（早い人が先）
     * @param target        精算対象者かどうか。対象外でも、購入者であれば結果の members に含まれる
     */
    public record Person(UUID participantId, String name, Instant createdAt, boolean target) {
    }

    /**
     * 計算の入力となる購入1件。購入者と実購入額が入力済みの商品だけを渡す。
     *
     * @param paidByParticipantId 立て替えた人（購入者）の参加者ID
     * @param amount              実購入額（円）
     */
    public record Purchase(UUID paidByParticipantId, int amount) {
    }

    /**
     * 参加者1人分の計算結果。
     *
     * @param participantId 参加者のID
     * @param name          参加者の名前
     * @param burdenAmount  負担額（円）。対象外の参加者は 0
     * @param paidAmount    立替額（円）。その人が購入者になっている実購入額の合計
     * @param balanceAmount 差額（円）= 立替額 − 負担額。プラスなら受け取る側、マイナスなら支払う側
     */
    public record MemberResult(UUID participantId, String name, int burdenAmount, int paidAmount, int balanceAmount) {
    }

    /**
     * 送金案1件（誰が誰にいくら送るか）。
     *
     * @param fromParticipantId 送金する人（支払う側）の参加者ID
     * @param toParticipantId   受け取る人の参加者ID。送金元と同じ人にはならない
     * @param amount            送金額（円）。1円以上（0円の送金案は作らない）
     */
    public record TransferResult(UUID fromParticipantId, UUID toParticipantId, int amount) {
    }

    /**
     * 計算結果の全体。
     *
     * @param actualTotalPrice 実購入額の合計（円）
     * @param members          参加者ごとの結果。対象者と、対象外でも購入者になっている人を、登録順（createdAt、同じなら
     *                         participantId）で並べる
     * @param transfers        送金案。支払う側の登録順、同じ支払い側の中では受け取る側の登録順で並べる
     */
    public record Result(int actualTotalPrice, List<MemberResult> members, List<TransferResult> transfers) {
    }

    public Result calculate(List<Person> people, List<Purchase> purchases) {
        int targetCount = 0;
        for(Person person : people) {
            if (person.target()) {
                targetCount += 1;
            }
        }
        if (targetCount == 0) {
            throw new IllegalArgumentException("精算対象者が0人です");
        }
        // createdAt -> participantIdの優先順位で並び替え
        List<Person> sortedPeople = people.stream().sorted(Comparator.comparing(Person::createdAt).thenComparing(Person::participantId)).toList();
        // 実購入額の合計
        long totalActualPurchaseAmount = purchases.stream().mapToLong(Purchase::amount).sum();
        // 基本負担額
        long baseContributionAmount = totalActualPurchaseAmount / targetCount;
        // 余り
        long surplus = totalActualPurchaseAmount - baseContributionAmount * targetCount;
        // 余りの数だけ、対象者の中で登録順の先頭から+1円ずつ配る
        int targetPeopleCount = 0;
        Map<UUID, Long> burdenAmountMap = new HashMap<>();
        for (Person person : sortedPeople) {
            if (person.target()) {
                if (targetPeopleCount < surplus) {
                    burdenAmountMap.put(person.participantId(), baseContributionAmount+1L);
                } else {
                    burdenAmountMap.put(person.participantId(), baseContributionAmount);
                }
                targetPeopleCount++;
            }
        }

        // 建て替え額の集計
        // <購入者のID, 建て替え額の合計>
        Map<UUID, Long> paidAmountMap = new HashMap<>();
        for (Purchase purchase : purchases) {
            paidAmountMap.merge(purchase.paidByParticipantId(), (long)purchase.amount(), Long::sum);
        }
        
        // membersの作成
        List<MemberResult> members = new ArrayList<>();
        for(Person person : sortedPeople) {
            if(person.target() || (paidAmountMap.getOrDefault(person.participantId(), 0L) != 0)) {
                // personの負担額
                long burdenAmount = burdenAmountMap.getOrDefault(person.participantId(), 0L);
                // personの立替額
                long paidAmount = paidAmountMap.getOrDefault(person.participantId(), 0L);

                // 差額
                long balanceAmount = paidAmount - burdenAmount;

                // membersに追加する
                members.add(
                    new MemberResult(person.participantId(), person.name(), (int)burdenAmount, (int)paidAmount, (int)balanceAmount));
            }
        }
        return new Result((int)totalActualPurchaseAmount, members, createTransfers(members));
    }
    private List<TransferResult> createTransfers(List<MemberResult> members) {
        List<TransferResult> transfers = new ArrayList<>();
        // 支払う人のID一覧
        List<UUID> payerIds = new ArrayList<>();
        // 支払う人の残額
        List<Long> payerRemainings = new ArrayList<>();

        // 受け取る人のID一覧
        List<UUID> receiverIds = new ArrayList<>();
        // 受け取る側の残額
        List<Long> receiverRemainings = new ArrayList<>();

        // 支払う側のインデックス番号
        int payerIndex = 0;
        // 受け取る側のインデックス番号
        int receiverIndex = 0;
        // 1回の送金額
        long transferAmount = 0L;
        
        for (MemberResult  member: members) {
            // balanceAmountが負なら支払う側、正なら受け取る側
            if (member.balanceAmount() < 0) {
                payerIds.add(member.participantId());
                payerRemainings.add((long)(member.balanceAmount()*-1));
            } else if (member.balanceAmount() > 0) {
                receiverIds.add(member.participantId());
                receiverRemainings.add((long)member.balanceAmount());
            }
        }

        while (payerIndex < payerIds.size() && receiverIndex < receiverIds.size()) {
            long payerRemaining = payerRemainings.get(payerIndex);
            long receiverRemaining = receiverRemainings.get(receiverIndex);
            UUID payerId = payerIds.get(payerIndex);
            UUID receiverId = receiverIds.get(receiverIndex);

            // 送金額
            transferAmount = Math.min(payerRemaining, receiverRemaining);
            transfers.add(new TransferResult(payerId, receiverId, (int)transferAmount));
            
            // 二人の残額を更新する
            payerRemainings.set(payerIndex, payerRemaining - transferAmount);
            receiverRemainings.set(receiverIndex, receiverRemaining - transferAmount);

            // 残額が０になった方のIndexを+1する
            if (payerRemaining - transferAmount == 0) {
                payerIndex++;
            }
            if (receiverRemaining - transferAmount == 0) {
                receiverIndex++;
            }
        }
        return transfers;

    }
}