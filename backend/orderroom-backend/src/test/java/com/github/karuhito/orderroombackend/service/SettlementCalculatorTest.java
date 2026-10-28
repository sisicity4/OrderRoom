package com.github.karuhito.orderroombackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.karuhito.orderroombackend.service.SettlementCalculator.MemberResult;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.Person;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.Purchase;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.Result;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.TransferResult;

/* DBに触れない計算クラスの単体テスト（Springは起動しない） */
class SettlementCalculatorTest {
    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    private final SettlementCalculator calculator = new SettlementCalculator();

    // ---- テスト用の部品 ----

    /* 参加者の文字（'A' など）から、順序が読みやすい固定のIDを作る */
    private static UUID uid(char name) {
        return new UUID(0L, name);
    }

    /* 精算対象者。secondsは参加登録の早さ（BASEからの秒数） */
    private static Person target(char name, int seconds) {
        return new Person(uid(name), String.valueOf(name), BASE.plusSeconds(seconds), true);
    }

    /* 精算対象外の参加者 */
    private static Person outsider(char name, int seconds) {
        return new Person(uid(name), String.valueOf(name), BASE.plusSeconds(seconds), false);
    }

    private static Purchase buy(char purchaser, int amount) {
        return new Purchase(uid(purchaser), amount);
    }

    /* members の1行分: 参加者 / 負担額 / 立替額 / 差額 */
    private static Tuple row(char name, int burden, int paid, int balance) {
        return tuple(uid(name), burden, paid, balance);
    }

    private static TransferResult transfer(char from, char to, int amount) {
        return new TransferResult(uid(from), uid(to), amount);
    }

    private static List<Tuple> rowsOf(Result result) {
        return result.members().stream()
            .map(m -> tuple(m.participantId(), m.burdenAmount(), m.paidAmount(), m.balanceAmount()))
            .toList();
    }

    // ---- 基本の計算 ----

    @Test
    void splitsEvenlyWhenDivisible() {
        // 3人で合計9000、Aが全額を立替
        Result result = calculator.calculate(
            List.of(target('A', 0), target('B', 1), target('C', 2)),
            List.of(buy('A', 9000)));

        assertThat(result.actualTotalPrice()).isEqualTo(9000);
        assertThat(result.members()).extracting(MemberResult::name).containsExactly("A", "B", "C");
        assertThat(rowsOf(result)).containsExactly(
            row('A', 3000, 9000, 6000),
            row('B', 3000, 0, -3000),
            row('C', 3000, 0, -3000));
        assertThat(result.transfers()).containsExactly(
            transfer('B', 'A', 3000),
            transfer('C', 'A', 3000));
    }

    @Test
    void givesRemainderToEarlierParticipantsOneYenEach() {
        // 合計10001を3人で割ると、余り2円が参加登録の早い2人に1円ずつ
        Result result = calculator.calculate(
            List.of(target('A', 0), target('B', 1), target('C', 2)),
            List.of(buy('A', 10001)));

        assertThat(rowsOf(result)).containsExactly(
            row('A', 3334, 10001, 6667),
            row('B', 3334, 0, -3334),
            row('C', 3333, 0, -3333));
        assertThat(result.transfers()).containsExactly(
            transfer('B', 'A', 3334),
            transfer('C', 'A', 3333));
    }

    @Test
    void ordersByParticipantIdWhenCreatedAtIsSame() {
        // 登録日時が同じなら participantId の順。入力の並びは逆順にしておく
        Result result = calculator.calculate(
            List.of(target('C', 0), target('B', 0), target('A', 0)),
            List.of(buy('A', 10001)));

        assertThat(rowsOf(result)).containsExactly(
            row('A', 3334, 10001, 6667),
            row('B', 3334, 0, -3334),
            row('C', 3333, 0, -3333));
    }

    @Test
    void doesNotDependOnInputOrder() {
        List<Person> participants = List.of(
            target('A', 0), target('B', 1), target('C', 2), target('D', 3));
        List<Purchase> purchases = List.of(
            buy('A', 4000), buy('B', 4000), buy('A', 2000));
        Result expected = calculator.calculate(participants, purchases);

        List<Person> reversedParticipants = new ArrayList<>(participants);
        Collections.reverse(reversedParticipants);
        List<Purchase> reversedPurchases = new ArrayList<>(purchases);
        Collections.reverse(reversedPurchases);
        List<Person> rotatedParticipants = new ArrayList<>(participants);
        Collections.rotate(rotatedParticipants, 2);

        assertThat(calculator.calculate(reversedParticipants, reversedPurchases)).isEqualTo(expected);
        assertThat(calculator.calculate(rotatedParticipants, purchases)).isEqualTo(expected);
    }

    @Test
    void sumsPaidAmountAcrossMultiplePurchasersAndPurchases() {
        // 合計9000。Aは2回で6000、Bは3000を立替。負担額は3000ずつ
        Result result = calculator.calculate(
            List.of(target('A', 0), target('B', 1), target('C', 2)),
            List.of(buy('A', 4000), buy('B', 3000), buy('A', 2000)));

        assertThat(rowsOf(result)).containsExactly(
            row('A', 3000, 6000, 3000),
            row('B', 3000, 3000, 0),
            row('C', 3000, 0, -3000));
        // 差額0のBは送金案に出ない
        assertThat(result.transfers()).containsExactly(transfer('C', 'A', 3000));
    }

    @Test
    void worksWithSingleTargetedParticipant() {
        // 対象者が1人。対象外のBは購入者でもないので members に含まれない
        Result result = calculator.calculate(
            List.of(target('A', 0), outsider('B', 1)),
            List.of(buy('A', 5000)));

        assertThat(rowsOf(result)).containsExactly(row('A', 5000, 5000, 0));
        assertThat(result.transfers()).isEmpty();
    }

    // ---- 対象外の購入者（決定事項1-a） ----

    @Test
    void includesOutsiderPurchaserWithZeroBurdenAndDistributesRemainderAmongTargetedOnly() {
        // 対象外のXが10001を立替。余り1円は対象者のA・Bの中だけで配る
        // 並びは登録順: A(0) → X(1) → B(2)
        Result result = calculator.calculate(
            List.of(target('A', 0), outsider('X', 1), target('B', 2)),
            List.of(buy('X', 10001)));

        assertThat(rowsOf(result)).containsExactly(
            row('A', 5001, 0, -5001),
            row('X', 0, 10001, 10001),
            row('B', 5000, 0, -5000));
        assertThat(result.transfers()).containsExactly(
            transfer('A', 'X', 5001),
            transfer('B', 'X', 5000));
    }

    @Test
    void everyTargetedParticipantPaysWhenAllPurchasersAreOutsiders() {
        // 購入者が全員対象外。対象者の3人が、対象外の2人へ送金する
        Result result = calculator.calculate(
            List.of(outsider('X', 0), outsider('Y', 1), target('A', 2), target('B', 3), target('C', 4)),
            List.of(buy('X', 6000), buy('Y', 3000)));

        assertThat(rowsOf(result)).containsExactly(
            row('X', 0, 6000, 6000),
            row('Y', 0, 3000, 3000),
            row('A', 3000, 0, -3000),
            row('B', 3000, 0, -3000),
            row('C', 3000, 0, -3000));
        assertThat(result.transfers()).containsExactly(
            transfer('A', 'X', 3000),
            transfer('B', 'X', 3000),
            transfer('C', 'Y', 3000));
    }

    // ---- 購入がない場合 ----

    @Test
    void returnsAllZeroWhenThereAreNoPurchases() {
        Result result = calculator.calculate(
            List.of(target('A', 0), target('B', 1), target('C', 2)),
            List.of());

        assertThat(result.actualTotalPrice()).isZero();
        assertThat(rowsOf(result)).containsExactly(
            row('A', 0, 0, 0),
            row('B', 0, 0, 0),
            row('C', 0, 0, 0));
        assertThat(result.transfers()).isEmpty();
    }

    // ---- 送金案の相殺 ----

    @Test
    void offsetsFromTheFrontAndSplitsOnePayerAcrossReceivers() {
        // 合計10000を4人で割ると2500ずつ。Aが6000、Bが4000を立替
        // 受取: A +3500, B +1500 / 支払: C -2500, D -2500
        // C→A 2500、D→A 1000（Aの残り）、D→B 1500
        Result result = calculator.calculate(
            List.of(target('A', 0), target('B', 1), target('C', 2), target('D', 3)),
            List.of(buy('A', 6000), buy('B', 4000)));

        assertThat(rowsOf(result)).containsExactly(
            row('A', 2500, 6000, 3500),
            row('B', 2500, 4000, 1500),
            row('C', 2500, 0, -2500),
            row('D', 2500, 0, -2500));
        assertThat(result.transfers()).containsExactly(
            transfer('C', 'A', 2500),
            transfer('D', 'A', 1000),
            transfer('D', 'B', 1500));
    }

    // ---- 整合条件（設計書8.3） ----

    static Stream<Arguments> consistencyScenarios() {
        return Stream.of(
            Arguments.of("割り切れる",
                List.of(target('A', 0), target('B', 1), target('C', 2)),
                List.of(buy('A', 9000))),
            Arguments.of("割り切れない",
                List.of(target('A', 0), target('B', 1), target('C', 2)),
                List.of(buy('B', 10001))),
            Arguments.of("複数の購入者",
                List.of(target('A', 0), target('B', 1), target('C', 2), target('D', 3)),
                List.of(buy('A', 6000), buy('B', 4000), buy('A', 1))),
            Arguments.of("対象外の購入者あり",
                List.of(target('A', 0), outsider('X', 1), target('B', 2), target('C', 3)),
                List.of(buy('X', 7777), buy('B', 1234))),
            Arguments.of("購入なし",
                List.of(target('A', 0), target('B', 1)),
                List.<Purchase>of()),
            Arguments.of("全員が同額を立替",
                List.of(target('A', 0), target('B', 1), target('C', 2)),
                List.of(buy('A', 3000), buy('B', 3000), buy('C', 3000))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consistencyScenarios")
    void keepsSettlementInvariants(String label, List<Person> participants, List<Purchase> purchases) {
        Result result = calculator.calculate(participants, purchases);
        long expectedTotal = purchases.stream().mapToLong(Purchase::amount).sum();

        long burdenSum = result.members().stream().mapToLong(MemberResult::burdenAmount).sum();
        long paidSum = result.members().stream().mapToLong(MemberResult::paidAmount).sum();
        long balanceSum = result.members().stream().mapToLong(MemberResult::balanceAmount).sum();
        long shortageSum = result.members().stream()
            .filter(m -> m.balanceAmount() < 0).mapToLong(m -> -m.balanceAmount()).sum();
        long excessSum = result.members().stream()
            .filter(m -> m.balanceAmount() > 0).mapToLong(MemberResult::balanceAmount).sum();
        long transferSum = result.transfers().stream().mapToLong(TransferResult::amount).sum();

        assertThat(result.actualTotalPrice()).isEqualTo(expectedTotal);
        assertThat(burdenSum).isEqualTo(expectedTotal);
        assertThat(paidSum).isEqualTo(expectedTotal);
        assertThat(balanceSum).isZero();
        assertThat(transferSum).isEqualTo(shortageSum).isEqualTo(excessSum);
        // 0円の送金案は作らない。自分から自分への送金もない
        assertThat(result.transfers()).allSatisfy(t -> {
            assertThat(t.amount()).isPositive();
            assertThat(t.fromParticipantId()).isNotEqualTo(t.toParticipantId());
        });
    }

    // ---- 決定性・異常系 ----

    @Test
    void returnsSameResultForSameInput() {
        List<Person> participants = List.of(
            target('A', 0), target('B', 1), outsider('X', 2), target('C', 3));
        List<Purchase> purchases = List.of(buy('X', 10001), buy('B', 999));

        assertThat(calculator.calculate(participants, purchases))
            .isEqualTo(calculator.calculate(participants, purchases));
    }

    @Test
    void throwsWhenNoParticipantIsTargeted() {
        assertThatThrownBy(() -> calculator.calculate(
                List.of(outsider('A', 0), outsider('B', 1)),
                List.of(buy('A', 1000))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsWhenThereAreNoParticipants() {
        assertThatThrownBy(() -> calculator.calculate(List.of(), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
