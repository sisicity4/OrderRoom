package com.github.karuhito.orderroombackend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.karuhito.orderroombackend.dto.SettlementResponse;
import com.github.karuhito.orderroombackend.dto.SettlementResponse.IncompletePurchaseItem;
import com.github.karuhito.orderroombackend.dto.SettlementResponse.MemberSummary;
import com.github.karuhito.orderroombackend.dto.SettlementResponse.TransferSummary;
import com.github.karuhito.orderroombackend.entity.Item;
import com.github.karuhito.orderroombackend.entity.ItemStatus;
import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Settlement;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.exception.NoSettlementMembersException;
import com.github.karuhito.orderroombackend.exception.NotSettlementPayerException;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.exception.SettlementNotFoundException;
import com.github.karuhito.orderroombackend.repository.ItemRepository;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementRepository;
import com.github.karuhito.orderroombackend.resolver.Operator;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.MemberResult;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.Person;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.Purchase;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.Result;
import com.github.karuhito.orderroombackend.service.SettlementCalculator.TransferResult;

@Service
public class SettlementService {
    private final RoomRepository roomRepository;
    private final ItemRepository itemRepository;
    private final ParticipantRepository participantRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementMemberService settlementMemberService;
    private final SettlementCalculator settlementCalculator = new SettlementCalculator();
    private final SettlementLockGuard settlementLockGuard;

    public SettlementService(RoomRepository roomRepository, ItemRepository itemRepository,
            ParticipantRepository participantRepository, SettlementRepository settlementRepository,
            SettlementMemberService settlementMemberService, SettlementLockGuard settlementLockGuard) {
        this.roomRepository = roomRepository;
        this.itemRepository = itemRepository;
        this.participantRepository = participantRepository;
        this.settlementRepository = settlementRepository;
        this.settlementMemberService = settlementMemberService;
        this.settlementLockGuard = settlementLockGuard;

    }

    private record Prepared(
            List<Participant> participants,
            List<IncompletePurchaseItem> incompletePurchaseItems,
            int actualTotalPrice,
            int targetCount,
            SettlementCalculator.Result result) {
    }

    private Prepared prepare(UUID roomId) {
        // ルームの全参加者を取得し、計算クラスと同じ「登録日時 → participantId」の順に揃える
        List<Participant> participants = participantRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .sorted(Comparator.comparing(Participant::getCreatedAt).thenComparing(Participant::getId))
                .toList();
        // 対象者を取り、Set<UUID>にする
        List<Participant> targetParticipants = settlementMemberService.getEffectiveMembers(roomId);
        Set<UUID> selectedIds = targetParticipants.stream().map(participant -> participant.getId())
                .collect(Collectors.toSet());

        // 全参加者をPersonに変換する
        List<Person> people = participants.stream().map(participant -> new Person(participant.getId(),
                participant.getName(), participant.getCreatedAt(), selectedIds.contains(participant.getId()))).toList();

        // 商品の振り分け
        // 全ての商品リスト
        List<Item> items = itemRepository.findByRoomId(roomId);

        // 計算対象の商品リスト
        List<Purchase> purchaseItems = new ArrayList<>();
        // 未入力の商品リスト
        List<IncompletePurchaseItem> incompletePurchaseItems = new ArrayList<>();
        List<Item> sortedItems = items.stream()
                .sorted(Comparator.comparing(Item::getCreatedAt).thenComparing(Item::getId)).toList();
        for (Item item : sortedItems) {
            if (item.getStatus() != ItemStatus.ACCEPTED || !item.isPurchased()) {
                continue;
            } else if (item.getActualPrice() == null || item.getPaidByParticipant() == null) {
                incompletePurchaseItems.add(new IncompletePurchaseItem(item.getId(), item.getName()));
            } else {
                purchaseItems.add(new Purchase(item.getPaidByParticipant().getId(), item.getActualPrice()));
            }
        }
        long total = purchaseItems.stream().mapToLong(Purchase::amount).sum();
        int actualTotalPrice = (int) total;

        int targetCount = targetParticipants.size();
        Result result = null;
        if (targetCount != 0) {
            result = settlementCalculator.calculate(people, purchaseItems);
        }
        return new Prepared(participants, incompletePurchaseItems, actualTotalPrice, targetCount, result);
    }

    /* 計算クラスの結果を、レスポンスの members の形に詰め替える */
    private List<MemberSummary> toMemberSummaries(List<MemberResult> members) {
        return members.stream()
                .map(member -> new MemberSummary(
                        member.participantId(),
                        member.name(),
                        member.burdenAmount(),
                        member.paidAmount(),
                        member.balanceAmount()))
                .toList();
    }

    /* 送金案を「送金元の登録順 → 送金先の登録順」に並べて、レスポンスの transfers の形にする */
    private List<TransferSummary> toTransferSummaries(List<Settlement> settlements, List<Participant> participants) {
        // 参加者ID → 登録順の番号（participants は登録順に並んでいる）
        Map<UUID, Integer> registrationOrder = new HashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            registrationOrder.put(participants.get(i).getId(), i);
        }
        return settlements.stream()
                .sorted(Comparator
                        .comparing((Settlement settlement) -> registrationOrder
                                .get(settlement.getFromParticipant().getId()))
                        .thenComparing(settlement -> registrationOrder.get(settlement.getToParticipant().getId())))
                .map(settlement -> new TransferSummary(
                        settlement.getId(),
                        settlement.getFromParticipant().getId(),
                        settlement.getToParticipant().getId(),
                        settlement.getAmount(),
                        settlement.getSettledAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(UUID roomId) {
        return buildSettlementResponse(roomId);
    }

    @Transactional
    public SettlementResponse recalculate(UUID roomId) {
        Room room = roomRepository.findByIdForUpdate(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
        Prepared prepared = prepare(roomId);
        if (prepared.targetCount() == 0) {
            throw new NoSettlementMembersException(roomId);
        }
        settlementRepository.deleteByRoomIdAndSettledAtIsNull(roomId);
        // 参加者をIDで引ける表を作る
        Map<UUID, Participant> participantMap = prepared.participants().stream()
                .collect(Collectors.toMap(Participant::getId, participant -> participant));
        // 計算結果の送金案を取り出す
        List<TransferResult> transferResults = prepared.result().transfers();

        List<Settlement> settlements = new ArrayList<>();
        for (TransferResult transferResult : transferResults) {
            Participant fromParticipant = participantMap.get(transferResult.fromParticipantId());
            Participant toParticipant = participantMap.get(transferResult.toParticipantId());
            int amount = transferResult.amount();
            settlements.add(new Settlement(room, fromParticipant, toParticipant, amount));
        }
        List<Settlement> savedSettlements = settlementRepository.saveAll(settlements);
        List<MemberSummary> members = toMemberSummaries(prepared.result().members());
        List<TransferSummary> transfers = toTransferSummaries(savedSettlements, prepared.participants());

        return new SettlementResponse(prepared.actualTotalPrice(), prepared.incompletePurchaseItems(), members,
                transfers, false);
    }

    private SettlementResponse buildSettlementResponse(UUID roomId) {
        Prepared prepared = prepare(roomId);
        boolean locked = settlementRepository.existsByRoomIdAndSettledAtIsNotNull(roomId);

        List<MemberSummary> members;
        List<TransferSummary> transfers;
        if (prepared.result() == null) {
            members = new ArrayList<>();
            transfers = new ArrayList<>();
        } else {
            members = toMemberSummaries(prepared.result().members());
            List<Settlement> settlements = settlementRepository.findByRoomId(roomId);
            transfers = toTransferSummaries(settlements, prepared.participants());
        }

        return new SettlementResponse(
                prepared.actualTotalPrice(),
                prepared.incompletePurchaseItems(),
                members,
                transfers,
                locked);
    }

    @Transactional
    public SettlementResponse updateSettled(UUID roomId, UUID settlementId, Operator operator, boolean settled) {
        settlementLockGuard.lockRoom(roomId);
        Settlement settlement = settlementRepository.findByIdAndRoomId(settlementId, roomId)
                .orElseThrow(() -> new SettlementNotFoundException(settlementId));
        if (!operator.isHost() && !settlement.getFromParticipant().getId().equals(operator.participantId())) {
            throw new NotSettlementPayerException(operator.participantId(), settlementId);
        }
        if (settled && settlement.getSettledAt() == null) {
            settlement.markSettled(Instant.now());
        }
        if (!settled) {
            settlement.markUnsettled();
        }
        return buildSettlementResponse(roomId);
    }

    @Transactional
    public SettlementResponse resetSettled(UUID roomId) {
        settlementLockGuard.lockRoom(roomId);
        List<Settlement> settlements = settlementRepository.findByRoomId(roomId);
        for (Settlement settlement : settlements) {
            settlement.markUnsettled();
        }
        return buildSettlementResponse(roomId);
    }
}
