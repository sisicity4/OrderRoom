package com.github.karuhito.orderroombackend.repository;

import com.github.karuhito.orderroombackend.entity.SettlementMember;
import com.github.karuhito.orderroombackend.entity.SettlementMemberId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementMemberRepository
    extends JpaRepository<SettlementMember, SettlementMemberId> {
  List<SettlementMember> findByIdRoomId(UUID roomId);
}
