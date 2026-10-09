package com.houcinee.bankapp.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.houcinee.bankapp.dto.event.TransferCompletedEvent;
import com.houcinee.bankapp.service.FraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransferEventConsumerTests {

	@Mock
	private FraudDetectionService fraudDetectionService;

	@Mock
	private Acknowledgment acknowledgment;

	private ObjectMapper objectMapper;
	private TransferEventConsumer consumer;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		consumer = new TransferEventConsumer(fraudDetectionService, objectMapper);
	}

	@Test
	void givenValidTransferEvent_whenConsumeTransferEvent_thenProcessesAndAcknowledges() throws Exception {
		UUID txId = UUID.randomUUID();
		UUID srcId = UUID.randomUUID();
		UUID destId = UUID.randomUUID();
		Instant now = Instant.now();

		TransferCompletedEvent event = new TransferCompletedEvent(txId, srcId, destId, 5000L, now);
		String payload = objectMapper.writeValueAsString(event);

		when(fraudDetectionService.acquireProcessingLock(txId)).thenReturn(true);
		when(fraudDetectionService.checkVelocityAndTrack(eq(srcId), eq(txId), any(Instant.class))).thenReturn(true);

		consumer.consumeTransferEvent(payload, acknowledgment);

		verify(fraudDetectionService).acquireProcessingLock(txId);
		verify(fraudDetectionService).checkVelocityAndTrack(eq(srcId), eq(txId), any(Instant.class));
		verify(fraudDetectionService).markCompleted(txId);
		verify(acknowledgment).acknowledge();
	}

	@Test
	void givenDuplicateEvent_whenConsumeTransferEvent_thenAcksImmediatelyWithoutVelocityCheck() throws Exception {
		UUID txId = UUID.randomUUID();
		UUID srcId = UUID.randomUUID();
		UUID destId = UUID.randomUUID();

		TransferCompletedEvent event = new TransferCompletedEvent(txId, srcId, destId, 5000L, Instant.now());
		String payload = objectMapper.writeValueAsString(event);

		when(fraudDetectionService.acquireProcessingLock(txId)).thenReturn(false);

		consumer.consumeTransferEvent(payload, acknowledgment);

		verify(fraudDetectionService).acquireProcessingLock(txId);
		verify(fraudDetectionService, never()).checkVelocityAndTrack(any(), any(), any());
		verify(fraudDetectionService, never()).markCompleted(any());
		verify(acknowledgment).acknowledge();
	}

	@Test
	void givenMalformedJson_whenConsumeTransferEvent_thenAcksToPreventPoisonPill() {
		String malformedJson = "{not-a-valid-json";

		consumer.consumeTransferEvent(malformedJson, acknowledgment);

		verify(fraudDetectionService, never()).acquireProcessingLock(any());
		verify(acknowledgment).acknowledge();
	}

	@Test
	void givenServiceException_whenConsumeTransferEvent_thenDoesNotAcknowledge() throws Exception {
		UUID txId = UUID.randomUUID();
		UUID srcId = UUID.randomUUID();
		UUID destId = UUID.randomUUID();

		TransferCompletedEvent event = new TransferCompletedEvent(txId, srcId, destId, 5000L, Instant.now());
		String payload = objectMapper.writeValueAsString(event);

		when(fraudDetectionService.acquireProcessingLock(txId)).thenReturn(true);
		when(fraudDetectionService.checkVelocityAndTrack(eq(srcId), eq(txId), any(Instant.class)))
				.thenThrow(new RuntimeException("Redis connection timeout"));

		consumer.consumeTransferEvent(payload, acknowledgment);

		verify(fraudDetectionService, never()).markCompleted(any());
		verify(acknowledgment, never()).acknowledge();
	}
}
