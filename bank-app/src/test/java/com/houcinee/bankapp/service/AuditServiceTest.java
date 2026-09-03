package com.houcinee.bankapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.enums.AuditAction;
import com.houcinee.bankapp.model.AuditLog;
import com.houcinee.bankapp.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.houcinee.bankapp.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

    @InjectMocks
    private AuditService auditService;


    @Test
    void givenTransferFailed_whenRecord_thenSavesCorrectAuditLogWithJson() {
        // Arrange
        UUID src = UUID.randomUUID();
        UUID dest = UUID.randomUUID();
        TransactionRequest txRequest = new TransactionRequest(src, dest, 500L);
        String reason = "Insufficient balance";

        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.5");

        // Act
        auditService.recordTransferFailed(txRequest, reason, request);

        // Assert
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        
        assertEquals(AuditAction.TRANSFER_FAILED, savedLog.getAction());
        assertEquals("TRANSACTION", savedLog.getTargetEntityType());
        assertEquals("192.168.1.5", savedLog.getIpAddress());
        assertNotNull(savedLog.getTimestamp());

        // Verify JSON details actually serialized correctly
        String details = savedLog.getDetails();
        assertTrue(details.contains(src.toString()));
        assertTrue(details.contains("Insufficient balance"));
        assertTrue(details.contains("500"));
    }
    
    @Test
    void givenObjectMapperFails_whenRecord_thenSwallowsExceptionAndDoesNotCrash() throws Exception {
        // Arrange
        AuditService brokenService = new AuditService(auditLogRepository);
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        
        // Inject the broken mock mapper using Reflection since it's instantiated inline
        ReflectionTestUtils.setField(brokenService, "objectMapper", brokenMapper);
        
        when(brokenMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Simulated JSON Exception"));
        
        // Act & Assert: The audit log failure MUST NOT throw an exception and crash the business flow
        assertDoesNotThrow(() -> brokenService.recordAccessDenied("/api/secret", request));
        
        // Verify it failed gracefully and didn't save corrupted data
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void givenFiltersAndPageable_whenGetAuditLogs_thenCallsRepositoryAndReturnsMappedPage() {
        // Arrange
        UUID actorId = UUID.randomUUID();
        AuditAction action = AuditAction.LOGIN_FAILURE;
        Instant now = Instant.now();
        Pageable pageable = PageRequest.of(0, 10);

        AuditLog dummyLog = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .targetEntityType("USER")
                .targetEntityId(actorId)
                .details("{\"reason\":\"Bad credentials\"}")
                .ipAddress("127.0.0.1")
                .correlationId("test-corr-id")
                .build();

        Page<AuditLog> logPage = new PageImpl<>(List.of(dummyLog), pageable, 1);
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(logPage);

        // Act
        Page<AuditLogResponse> responsePage = auditService.getAuditLogs(actorId, action, now, now, pageable);

        // Assert
        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        AuditLogResponse response = responsePage.getContent().get(0);
        assertEquals(actorId, response.getActorId());
        assertEquals(action, response.getAction());
        assertEquals("USER", response.getTargetEntityType());
        assertEquals("test-corr-id", response.getCorrelationId());
        verify(auditLogRepository).findAll(any(Specification.class), eq(pageable));
    }
}
