package com.houcinee.bankapp.dto;

import com.houcinee.bankapp.enums.AuditAction;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {
    private UUID id;
    private UUID actorId;
    private AuditAction action;
    private String targetEntityType;
    private UUID targetEntityId;
    private String details;
    private String ipAddress;
    private String correlationId;
    private Instant timestamp;
}
