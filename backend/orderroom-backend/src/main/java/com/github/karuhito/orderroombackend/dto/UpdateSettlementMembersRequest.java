package com.github.karuhito.orderroombackend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.UniqueElements;

public record UpdateSettlementMembersRequest(
    @NotEmpty @UniqueElements(message = "参加者IDを重複して指定できません") List<@NotNull UUID> participantIds) {}
