package com.houcinee.bankapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.enums.AuditAction;
import com.houcinee.bankapp.model.AuditLog;
import com.houcinee.bankapp.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void recordLoginSuccess(HttpServletRequest request) {
        saveLog(getCurrentUserId(), AuditAction.LOGIN_SUCCESS, null, null, Map.of("status", "success"), request);
    }

    public void recordLoginFailure(String attemptedEmail, String reason, HttpServletRequest request) {
        Map<String, Object> details = Map.of(
                "attemptedEmail", attemptedEmail != null ? attemptedEmail : "UNKNOWN",
                "reason", reason
        );
        saveLog(null, AuditAction.LOGIN_FAILURE, null, null, details, request);
    }

    public void recordAccessDenied(String uri, HttpServletRequest request) {
        Map<String, Object> details = Map.of("uri", uri);
        saveLog(getCurrentUserId(), AuditAction.ACCESS_DENIED, null, null, details, request);
    }

    public void recordUserRegistered(UUID newUserId, String email, HttpServletRequest request) {
        Map<String, Object> details = Map.of("email", email);
        saveLog(newUserId, AuditAction.USER_REGISTERED, "USER", newUserId, details, request);
    }

    public void recordAccountCreated(UUID accountId, String currency, HttpServletRequest request) {
        Map<String, Object> details = Map.of("currency", currency);
        saveLog(getCurrentUserId(), AuditAction.ACCOUNT_CREATED, "ACCOUNT", accountId, details, request);
    }

    public void recordTransferExecuted(TransactionResponse response, HttpServletRequest request) {
        Map<String, Object> details = Map.of(
                "sourceAccountId", response.getSourceAccountId(),
                "destinationAccountId", response.getDestinationAccountId(),
                "amount", response.getAmount(),
                "status", response.getStatus().name()
        );
        saveLog(getCurrentUserId(), AuditAction.TRANSFER_EXECUTED, "TRANSACTION", response.getId(), details, request);
    }

    public void recordTransferFailed(TransactionRequest transactionRequest, String reason, HttpServletRequest request) {
        Map<String, Object> details = Map.of(
                "sourceAccountId", transactionRequest.getSrcAccount(),
                "destinationAccountId", transactionRequest.getDestAccount(),
                "amount", transactionRequest.getAmount(),
                "reason", reason
        );
        saveLog(getCurrentUserId(), AuditAction.TRANSFER_FAILED, "TRANSACTION", null, details, request);
    }

    private void saveLog(UUID actorId, AuditAction action, String entityType, UUID entityId, Map<String, Object> detailsMap, HttpServletRequest request) {
        try {
            String detailsJson = objectMapper.writeValueAsString(detailsMap);
            String clientIp = extractClientIp(request);
            String correlationId = MDC.get("correlationId");

            AuditLog logEntry = AuditLog.builder()
                    .actorId(actorId)
                    .action(action)
                    .targetEntityType(entityType)
                    .targetEntityId(entityId)
                    .details(detailsJson)
                    .ipAddress(clientIp)
                    .correlationId(correlationId != null ? correlationId : "SYSTEM")
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            // Never let audit logging crash the main business flow
            log.error("Failed to save audit log for action: {}", action, e);
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        return null; // Unauthenticated actions
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        return (xForwardedFor != null && !xForwardedFor.isBlank()) ? xForwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }
}
