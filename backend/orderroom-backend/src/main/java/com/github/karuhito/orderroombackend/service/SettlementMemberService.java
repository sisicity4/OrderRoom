package com.github.karuhito.orderroombackend.service;

import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.SettlementMember;
import com.github.karuhito.orderroombackend.exception.ParticipantNotFoundException;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;
import com.github.karuhito.orderroombackend.repository.RoomRepository;
import com.github.karuhito.orderroombackend.repository.SettlementMemberRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementMemberService {
  private final RoomRepository roomRepository;
  private final ParticipantRepository participantRepository;
  private final SettlementMemberRepository settlementMemberRepository;

  public SettlementMemberService(
      RoomRepository roomRepository,
      ParticipantRepository participantRepository,
      SettlementMemberRepository settlementMemberRepository) {
    this.roomRepository = roomRepository;
    this.participantRepository = participantRepository;
    this.settlementMemberRepository = settlementMemberRepository;
  }

  /** 検証済みのID一覧を要求順で受け取り、全件を置換する。 */
  @Transactional
  public List<UUID> replaceMembers(UUID roomId, List<UUID> participantIds) {
    if (participantIds == null
        || participantIds.isEmpty()
        || participantIds.stream().anyMatch(java.util.Objects::isNull)) {
      throw new IllegalArgumentException("精算対象者を1人以上指定してください");
    }
    if (participantIds.stream().distinct().count() != participantIds.size()) {
      throw new IllegalArgumentException("参加者IDを重複して指定できません");
    }
    // 同じルームへの置換を直列化し、同時更新による集合の混在を防ぐ。
    roomRepository.findByIdForUpdate(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
    Map<UUID, Participant> roomParticipants =
        participantRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
            .collect(Collectors.toMap(Participant::getId, Function.identity()));
    List<Participant> participants =
        participantIds.stream()
            .map(
                id -> {
                  Participant participant = roomParticipants.get(id);
                  if (participant == null) {
                    throw new ParticipantNotFoundException(id);
                  }
                  return participant;
                })
            .toList();

    // 全員を検証してから削除。同じ複合PKを再登録する前にDELETEを確定させる。
    settlementMemberRepository.deleteAll(settlementMemberRepository.findByIdRoomId(roomId));
    settlementMemberRepository.flush();
    settlementMemberRepository.saveAll(participants.stream().map(SettlementMember::new).toList());
    return participants.stream().map(Participant::getId).toList();
  }

  @Transactional(readOnly = true)
  public List<Participant> getEffectiveMembers(UUID roomId) {
    if (!roomRepository.existsById(roomId)) {
      throw new RoomNotFoundException(roomId);
    }
    List<SettlementMember> members = settlementMemberRepository.findByIdRoomId(roomId);
    List<Participant> participants = participantRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    if (members.isEmpty()) {
      // 既定値を保存しない。未設定なら後から参加した人も対象になる。
      return participants;
    }
    Set<UUID> selectedIds =
        members.stream()
            .map(member -> member.getId().getParticipantId())
            .collect(Collectors.toSet());
    return participants.stream().filter(p -> selectedIds.contains(p.getId())).toList();
  }
}
