package com.houcinee.bankapp.service;

import com.houcinee.bankapp.model.OutboxEvent;
import com.houcinee.bankapp.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OutboxRelayServiceTests {

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private OutboxRelayService outboxRelayService;

	private final String transferTopic = "bank.transfers.v1";

	@BeforeEach
	void setUp() {
		outboxRelayService = new OutboxRelayService(outboxEventRepository, kafkaTemplate, transferTopic);
	}

	@Test
	void givenPendingEvents_whenPublishPendingEvents_thenSendsToKafkaAndMarksProcessed() {
		UUID accountId = UUID.randomUUID();
		OutboxEvent event1 = new OutboxEvent("ACCOUNT", accountId, "TRANSFER_COMPLETED", "{\"amount\":100}");
		ReflectionTestUtils.setField(event1, "id", UUID.randomUUID());

		OutboxEvent event2 = new OutboxEvent("ACCOUNT", accountId, "TRANSFER_COMPLETED", "{\"amount\":200}");
		ReflectionTestUtils.setField(event2, "id", UUID.randomUUID());

		when(outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc())
				.thenReturn(List.of(event1, event2));

		outboxRelayService.publishPendingEvents();

		verify(kafkaTemplate).send(eq(transferTopic), eq(accountId.toString()), eq("{\"amount\":100}"));
		verify(kafkaTemplate).send(eq(transferTopic), eq(accountId.toString()), eq("{\"amount\":200}"));
		assertTrue(event1.getProcessed());
		assertTrue(event2.getProcessed());
		verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
	}

	@Test
	void givenNoPendingEvents_whenPublishPendingEvents_thenDoesNothing() {
		when(outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc())
				.thenReturn(Collections.emptyList());

		outboxRelayService.publishPendingEvents();

		verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
		verify(outboxEventRepository, never()).save(any());
	}

	@Test
	void givenKafkaFailure_whenPublishPendingEvents_thenHaltsBatchToPreserveOrdering() {
		UUID accountId = UUID.randomUUID();
		OutboxEvent event1 = new OutboxEvent("ACCOUNT", accountId, "TRANSFER_COMPLETED", "{\"amount\":100}");
		ReflectionTestUtils.setField(event1, "id", UUID.randomUUID());

		OutboxEvent event2 = new OutboxEvent("ACCOUNT", accountId, "TRANSFER_COMPLETED", "{\"amount\":200}");
		ReflectionTestUtils.setField(event2, "id", UUID.randomUUID());

		when(outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc())
				.thenReturn(List.of(event1, event2));
		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
				.thenThrow(new RuntimeException("Kafka broker offline"));

		outboxRelayService.publishPendingEvents();

		// Should attempt event 1, fail, and break before attempting event 2
		verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
		assertFalse(event1.getProcessed());
		assertFalse(event2.getProcessed());
		verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
	}
}
