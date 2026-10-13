package com.houcinee.bankapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.houcinee.bankapp.dto.AuditLogResponse;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.enums.AuditAction;
import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.model.AuditLog;
import com.houcinee.bankapp.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest request;

    @org.mockito.Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private AuditService auditService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    private void authenticateUser(UUID userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void recordLoginSuccess_withAuthenticatedUser_savesAuditLog() {
        UUID userId = UUID.randomUUID();
        authenticateUser(userId);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        auditService.recordLoginSuccess(request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(userId, saved.getActorId());
        assertEquals(AuditAction.LOGIN_SUCCESS, saved.getAction());
        assertEquals("10.0.0.1", saved.getIpAddress());
        assertTrue(saved.getDetails().contains("\"status\":\"success\""));
    }

    @Test
    void recordLoginFailure_withGivenEmailAndReason_savesAuditLog() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        auditService.recordLoginFailure("user@bank.com", "Bad credentials", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertNull(saved.getActorId());
        assertEquals(AuditAction.LOGIN_FAILURE, saved.getAction());
        assertEquals("192.168.1.100", saved.getIpAddress());
        assertTrue(saved.getDetails().contains("user@bank.com"));
        assertTrue(saved.getDetails().contains("Bad credentials"));
    }

    @Test
    void recordLoginFailure_withNullEmail_defaultsToUnknown() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordLoginFailure(null, "Account locked", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertTrue(saved.getDetails().contains("UNKNOWN"));
        assertTrue(saved.getDetails().contains("Account locked"));
    }

    @Test
    void recordAccessDenied_withAuthenticatedUser_savesAuditLog() {
        UUID userId = UUID.randomUUID();
        authenticateUser(userId);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordAccessDenied("/api/v1/admin/secret", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(userId, saved.getActorId());
        assertEquals(AuditAction.ACCESS_DENIED, saved.getAction());
        assertTrue(saved.getDetails().contains("/api/v1/admin/secret"));
    }

    @Test
    void recordUserRegistered_savesAuditLogWithUserIdAndEmail() {
        UUID newUserId = UUID.randomUUID();
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordUserRegistered(newUserId, "newuser@bank.com", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(newUserId, saved.getActorId());
        assertEquals(AuditAction.USER_REGISTERED, saved.getAction());
        assertEquals("USER", saved.getTargetEntityType());
        assertEquals(newUserId, saved.getTargetEntityId());
        assertTrue(saved.getDetails().contains("newuser@bank.com"));
    }

    @Test
    void recordAccountCreated_savesAuditLogWithAccountIdAndCurrency() {
        UUID actorId = UUID.randomUUID();
        authenticateUser(actorId);
        UUID accountId = UUID.randomUUID();
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordAccountCreated(accountId, "USD", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(actorId, saved.getActorId());
        assertEquals(AuditAction.ACCOUNT_CREATED, saved.getAction());
        assertEquals("ACCOUNT", saved.getTargetEntityType());
        assertEquals(accountId, saved.getTargetEntityId());
        assertTrue(saved.getDetails().contains("USD"));
    }

    @Test
    void recordTransferExecuted_savesAuditLogWithTransactionDetails() {
        UUID actorId = UUID.randomUUID();
        authenticateUser(actorId);
        UUID txId = UUID.randomUUID();
        UUID srcAccount = UUID.randomUUID();
        UUID destAccount = UUID.randomUUID();

        TransactionResponse txResponse = TransactionResponse.builder()
                .id(txId)
                .sourceAccountId(srcAccount)
                .destinationAccountId(destAccount)
                .amount("100.00")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordTransferExecuted(txResponse, request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(actorId, saved.getActorId());
        assertEquals(AuditAction.TRANSFER_EXECUTED, saved.getAction());
        assertEquals("TRANSACTION", saved.getTargetEntityType());
        assertEquals(txId, saved.getTargetEntityId());
        assertTrue(saved.getDetails().contains(srcAccount.toString()));
        assertTrue(saved.getDetails().contains(destAccount.toString()));
        assertTrue(saved.getDetails().contains("100.00"));
        assertTrue(saved.getDetails().contains("COMPLETED"));
    }

    @Test
    void recordTransferFailed_whenRecord_thenSavesCorrectAuditLogWithJson() {
        UUID src = UUID.randomUUID();
        UUID dest = UUID.randomUUID();
        TransactionRequest txRequest = new TransactionRequest(src, dest, 500L);
        String reason = "Insufficient balance";

        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.5");

        auditService.recordTransferFailed(txRequest, reason, request);

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertEquals(AuditAction.TRANSFER_FAILED, savedLog.getAction());
        assertEquals("TRANSACTION", savedLog.getTargetEntityType());
        assertNull(savedLog.getTargetEntityId());
        assertEquals("192.168.1.5", savedLog.getIpAddress());

        String details = savedLog.getDetails();
        assertTrue(details.contains(src.toString()));
        assertTrue(details.contains("Insufficient balance"));
        assertTrue(details.contains("500"));
    }

    @Test
    void recordCardCreated_savesAuditLogWithCardDetails() {
        UUID actorId = UUID.randomUUID();
        authenticateUser(actorId);
        UUID cardId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordCardCreated(cardId, "DEBIT", accountId, request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(actorId, saved.getActorId());
        assertEquals(AuditAction.CARD_CREATED, saved.getAction());
        assertEquals("CARD", saved.getTargetEntityType());
        assertEquals(cardId, saved.getTargetEntityId());
        assertTrue(saved.getDetails().contains("DEBIT"));
        assertTrue(saved.getDetails().contains(accountId.toString()));
    }

    @Test
    void recordCardBlocked_savesAuditLogWithStatusBlocked() {
        UUID actorId = UUID.randomUUID();
        authenticateUser(actorId);
        UUID cardId = UUID.randomUUID();

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordCardBlocked(cardId, request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(actorId, saved.getActorId());
        assertEquals(AuditAction.CARD_BLOCKED, saved.getAction());
        assertEquals("CARD", saved.getTargetEntityType());
        assertEquals(cardId, saved.getTargetEntityId());
        assertTrue(saved.getDetails().contains("BLOCKED"));
    }

    @Test
    void recordCardActivated_savesAuditLogWithStatusActive() {
        UUID actorId = UUID.randomUUID();
        authenticateUser(actorId);
        UUID cardId = UUID.randomUUID();

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordCardActivated(cardId, request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(actorId, saved.getActorId());
        assertEquals(AuditAction.CARD_ACTIVATED, saved.getAction());
        assertEquals("CARD", saved.getTargetEntityType());
        assertEquals(cardId, saved.getTargetEntityId());
        assertTrue(saved.getDetails().contains("ACTIVE"));
    }

    @Test
    void extractClientIp_withMultipleCommaSeparatedIps_extractsFirstIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");

        auditService.recordLoginFailure("test@bank.com", "Invalid password", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("203.0.113.195", captor.getValue().getIpAddress());
    }

    @Test
    void extractClientIp_withBlankHeader_fallsBackToRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("10.10.10.10");

        auditService.recordLoginFailure("test@bank.com", "Invalid password", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("10.10.10.10", captor.getValue().getIpAddress());
    }

    @Test
    void extractClientIp_withNullRequest_returnsUnknown() {
        auditService.recordLoginFailure("test@bank.com", "Invalid password", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("UNKNOWN", captor.getValue().getIpAddress());
    }

    @Test
    void saveLog_withCorrelationIdInMDC_usesMdcCorrelationId() {
        MDC.put("correlationId", "custom-corr-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordLoginFailure("test@bank.com", "Failure", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("custom-corr-123", captor.getValue().getCorrelationId());
    }

    @Test
    void getCurrentUserId_whenPrincipalNotUuid_returnsNull() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("non-uuid-principal", "pass");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordLoginSuccess(request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getActorId());
    }

    @Test
    void getCurrentUserId_whenNotAuthenticated_returnsNull() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(UUID.randomUUID(), "pass");
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        auditService.recordLoginSuccess(request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getActorId());
    }

    @Test
    void givenObjectMapperFails_whenRecord_thenSwallowsExceptionAndDoesNotCrash() throws Exception {
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        AuditService brokenService = new AuditService(auditLogRepository, brokenMapper);

        when(brokenMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Simulated JSON Exception"));

        assertDoesNotThrow(() -> brokenService.recordAccessDenied("/api/secret", request));
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void givenFiltersAndPageable_whenGetAuditLogs_thenCallsRepositoryAndReturnsMappedPage() {
        UUID actorId = UUID.randomUUID();
        AuditAction action = AuditAction.LOGIN_FAILURE;
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        Pageable pageable = PageRequest.of(0, 10);
        UUID logId = UUID.randomUUID();

        AuditLog dummyLog = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .targetEntityType("USER")
                .targetEntityId(actorId)
                .details("{\"reason\":\"Bad credentials\"}")
                .ipAddress("127.0.0.1")
                .correlationId("test-corr-id")
                .build();
        ReflectionTestUtils.setField(dummyLog, "id", logId);
        ReflectionTestUtils.setField(dummyLog, "timestamp", start);

        Page<AuditLog> logPage = new PageImpl<>(List.of(dummyLog), pageable, 1);
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(logPage);

        Page<AuditLogResponse> responsePage = auditService.getAuditLogs(actorId, action, start, end, pageable);

        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        AuditLogResponse response = responsePage.getContent().get(0);
        assertEquals(logId, response.getId());
        assertEquals(actorId, response.getActorId());
        assertEquals(action, response.getAction());
        assertEquals("USER", response.getTargetEntityType());
        assertEquals(actorId, response.getTargetEntityId());
        assertEquals("test-corr-id", response.getCorrelationId());
        assertEquals("127.0.0.1", response.getIpAddress());
        assertEquals(start, response.getTimestamp());
        verify(auditLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void givenNullFilters_whenGetAuditLogs_thenCallsRepositoryWithConjunction() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> logPage = new PageImpl<>(List.of(), pageable, 0);
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(logPage);

        Page<AuditLogResponse> responsePage = auditService.getAuditLogs(null, null, null, null, pageable);

        assertNotNull(responsePage);
        assertEquals(0, responsePage.getTotalElements());
        verify(auditLogRepository).findAll(any(Specification.class), eq(pageable));
    }
}
