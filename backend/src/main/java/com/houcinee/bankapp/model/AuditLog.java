package com.houcinee.bankapp.model;

import com.houcinee.bankapp.enums.AuditAction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "actor_id") // Nullable for failed logins
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(name = "target_entity_type") // Nullable
    private String targetEntityType;

    @Column(name = "target_entity_id") // Nullable
    private UUID targetEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String details;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Builder
    public AuditLog(UUID actorId, AuditAction action, String targetEntityType, UUID targetEntityId, String details, String ipAddress, String correlationId) {
        this.actorId = actorId;
        this.action = action;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.correlationId = correlationId;
        this.timestamp = Instant.now(); // Automatically set upon creation
    }
}
