package com.houcinee.bankapp.service;

import com.houcinee.bankapp.model.OutboxEvent;
import com.houcinee.bankapp.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class OutboxRelayService {

	private final OutboxEventRepository outboxEventRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final String transferTopic;

	public OutboxRelayService(
			OutboxEventRepository outboxEventRepository,
			KafkaTemplate<String, String> kafkaTemplate,
			@Value("${app.outbox.relay.topic-transfers:bank.transfers.v1}") String transferTopic) {
		this.outboxEventRepository = outboxEventRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.transferTopic = transferTopic;
	}

	@Scheduled(fixedDelayString = "${app.outbox.relay.delay-ms:500}")
	@Transactional
	public void publishPendingEvents() {
		List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();
		if (pendingEvents.isEmpty()) {
			return;
		}

		log.debug("Found {} pending outbox events to relay to Kafka", pendingEvents.size());

		for (OutboxEvent event : pendingEvents) {
			try {
				String topic = resolveTopic(event);
				String key = event.getAggregateId().toString();

				kafkaTemplate.send(topic, key, event.getPayload());
				event.markProcessed();
				outboxEventRepository.save(event);

				log.info("Relayed outbox event {} of type {} to topic {} with key {}",
						event.getId(), event.getEventType(), topic, key);
			} catch (Exception e) {
				log.error("Failed to publish outbox event {} to Kafka. Halting batch to preserve order.", event.getId(), e);
				// Break out of the loop so we don't process subsequent events out-of-order
				break;
			}
		}
	}

	private String resolveTopic(OutboxEvent event) {
		if ("ACCOUNT".equalsIgnoreCase(event.getAggregateType())) {
			return transferTopic;
		}
		return "bank.general-events.v1";
	}
}
