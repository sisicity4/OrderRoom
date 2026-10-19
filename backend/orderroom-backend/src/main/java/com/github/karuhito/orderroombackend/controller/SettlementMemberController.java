package com.github.karuhito.orderroombackend.controller;

import com.github.karuhito.orderroombackend.dto.SettlementMembersResponse;
import com.github.karuhito.orderroombackend.dto.UpdateSettlementMembersRequest;
import com.github.karuhito.orderroombackend.service.SettlementMemberService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/settlement-members")
public class SettlementMemberController {
  private final SettlementMemberService service;

  public SettlementMemberController(SettlementMemberService service) {
    this.service = service;
  }

  @PutMapping
  public SettlementMembersResponse replaceMembers(
      @PathVariable UUID roomId, @Valid @RequestBody UpdateSettlementMembersRequest request) {
    return new SettlementMembersResponse(service.replaceMembers(roomId, request.participantIds()));
  }
}
