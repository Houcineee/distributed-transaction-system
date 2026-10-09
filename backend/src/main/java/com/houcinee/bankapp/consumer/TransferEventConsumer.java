package com.houcinee.bankapp.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.event.TransferCompletedEvent;
import com.houcinee.bankapp.service.FraudDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransferEventConsumer {

	private final FraudDetectionService fraudDetectionService;
	private final ObjectMapper objectMapper;

	public TransferEventConsumer(FraudDetectionService fraudDetectionService, ObjectMapper objectMapper) {
		this.fraudDetectionService = fraudDetectionService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(
			topics = "${app.outbox.relay.topic-transfers:bank.transfers.v1}",
			groupId = "${spring.kafka.consumer.group-id:bank-fraud-engine}"
	)
	public void consumeTransferEvent(String payload, Acknowledgment ack) {
		TransferCompletedEvent event;
		try {
			event = objectMapper.readValue(payload, TransferCompletedEvent.class);
		} catch (Exception e) {
			log.error("Failed to deserialize transfer event payload: {}", payload, e);
			ack.acknowledge();
			return;
		}

		boolean isNew = fraudDetectionService.acquireProcessingLock(event.transactionId());
		if (!isNew) {
			log.info("Duplicate or in-flight transfer event {} detected. Skipping processing.", event.transactionId());
			ack.acknowledge();
			return;
		}

		try {
			boolean isClean = fraudDetectionService.checkVelocityAndTrack(
					event.sourceAccountId(),
					event.transactionId(),
					event.timestamp()
			);

			if (!isClean) {
				log.warn("[FRAUD_FLAG] Account {} flagged for velocity violation on transaction {}",
						event.sourceAccountId(), event.transactionId());
			}

			fraudDetectionService.markCompleted(event.transactionId());
			ack.acknowledge();
		} catch (Exception e) {
			log.error("Failed to process fraud check for transaction {}. Offset not committed for retry.",
					event.transactionId(), e);
		}
	}
}
