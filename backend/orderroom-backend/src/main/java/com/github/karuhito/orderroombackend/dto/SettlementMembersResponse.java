package com.github.karuhito.orderroombackend.dto;

import java.util.List;
import java.util.UUID;

public record SettlementMembersResponse(List<UUID> participantIds) {}
