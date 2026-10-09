package com.houcinee.bankapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FraudDetectionServiceTests {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private ZSetOperations<String, String> zSetOperations;

	private FraudDetectionService fraudDetectionService;

	private final long windowSeconds = 60;
	private final long maxTransfers = 3;

	@BeforeEach
	void setUp() {
		fraudDetectionService = new FraudDetectionService(redisTemplate, windowSeconds, maxTransfers);
	}

	@Test
	void givenNewEvent_whenAcquireProcessingLock_thenReturnsTrue() {
		UUID eventId = UUID.randomUUID();
		String expectedKey = "fraud:processed_event:" + eventId;

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(eq(expectedKey), eq("PROCESSING"), eq(Duration.ofSeconds(30))))
				.thenReturn(Boolean.TRUE);

		boolean acquired = fraudDetectionService.acquireProcessingLock(eventId);

		assertTrue(acquired);
		verify(valueOperations).setIfAbsent(expectedKey, "PROCESSING", Duration.ofSeconds(30));
	}

	@Test
	void givenDuplicateOrInFlightEvent_whenAcquireProcessingLock_thenReturnsFalse() {
		UUID eventId = UUID.randomUUID();
		String expectedKey = "fraud:processed_event:" + eventId;

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(eq(expectedKey), eq("PROCESSING"), eq(Duration.ofSeconds(30))))
				.thenReturn(Boolean.FALSE);

		boolean acquired = fraudDetectionService.acquireProcessingLock(eventId);

		assertFalse(acquired);
	}

	@Test
	void givenTransfersWithinLimit_whenCheckVelocityAndTrack_thenReturnsTrue() {
		UUID accountId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		Instant now = Instant.now();
		String expectedKey = "fraud:velocity:account:" + accountId;

		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(zSetOperations.size(expectedKey)).thenReturn(3L);

		boolean isClean = fraudDetectionService.checkVelocityAndTrack(accountId, eventId, now);

		assertTrue(isClean);
		verify(zSetOperations).removeRangeByScore(eq(expectedKey), eq(0.0), anyDouble());
		verify(zSetOperations).add(eq(expectedKey), eq(eventId.toString()), eq((double) now.toEpochMilli()));
		verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofSeconds(120)));
	}

	@Test
	void givenTransfersExceedingLimit_whenCheckVelocityAndTrack_thenReturnsFalseAndFlagsFraud() {
		UUID accountId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		Instant now = Instant.now();
		String expectedKey = "fraud:velocity:account:" + accountId;

		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(zSetOperations.size(expectedKey)).thenReturn(4L);

		boolean isClean = fraudDetectionService.checkVelocityAndTrack(accountId, eventId, now);

		assertFalse(isClean);
		verify(zSetOperations).removeRangeByScore(eq(expectedKey), eq(0.0), anyDouble());
		verify(zSetOperations).add(eq(expectedKey), eq(eventId.toString()), eq((double) now.toEpochMilli()));
	}

	@Test
	void givenEventId_whenMarkCompleted_thenSetsPermanentStatusWith24hTtl() {
		UUID eventId = UUID.randomUUID();
		String expectedKey = "fraud:processed_event:" + eventId;

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		fraudDetectionService.markCompleted(eventId);

		verify(valueOperations).set(expectedKey, "COMPLETED", Duration.ofHours(24));
	}
}
