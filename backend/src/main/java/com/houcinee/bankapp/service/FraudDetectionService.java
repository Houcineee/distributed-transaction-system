package com.houcinee.bankapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class FraudDetectionService {

	private final StringRedisTemplate redisTemplate;
	private final long windowSeconds;
	private final long maxTransfers;

	public FraudDetectionService(
			StringRedisTemplate redisTemplate,
			@Value("${app.fraud.velocity.window-seconds:60}") long windowSeconds,
			@Value("${app.fraud.velocity.max-transfers:3}") long maxTransfers) {
		this.redisTemplate = redisTemplate;
		this.windowSeconds = windowSeconds;
		this.maxTransfers = maxTransfers;
	}

	public boolean acquireProcessingLock(UUID eventId) {
		String key = "fraud:processed_event:" + eventId;
		Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", Duration.ofSeconds(30));
		return Boolean.TRUE.equals(acquired);
	}

	public boolean checkVelocityAndTrack(UUID accountId, UUID eventId, Instant timestamp) {
		String key = "fraud:velocity:account:" + accountId;
		long eventTimeMs = timestamp.toEpochMilli();
		long cutoffTimeMs = eventTimeMs - (windowSeconds * 1000L);

		redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoffTimeMs - 1);
		redisTemplate.opsForZSet().add(key, eventId.toString(), eventTimeMs);
		redisTemplate.expire(key, Duration.ofSeconds(windowSeconds * 2));

		Long count = redisTemplate.opsForZSet().size(key);
		long currentCount = (count != null) ? count : 0L;

		if (currentCount > maxTransfers) {
			log.warn("[FRAUD_ALERT] High Velocity Detected! Account {} made {} transfers within {}s (Limit: {})",
					accountId, currentCount, windowSeconds, maxTransfers);
			return false;
		}

		log.info("Velocity check passed for account {}. Current window count: {}/{}",
				accountId, currentCount, maxTransfers);
		return true;
	}

	public void markCompleted(UUID eventId) {
		String key = "fraud:processed_event:" + eventId;
		redisTemplate.opsForValue().set(key, "COMPLETED", Duration.ofHours(24));
	}
}
