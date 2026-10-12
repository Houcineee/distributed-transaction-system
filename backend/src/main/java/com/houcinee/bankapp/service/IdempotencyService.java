package com.houcinee.bankapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.IdempotencyRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

	private static final Duration PROCESSING_TTL = Duration.ofSeconds(30);
	private static final Duration COMPLETED_TTL = Duration.ofHours(24);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public String buildKey(String userId, String idempotencyKey) {
		return "idempotency:" + userId + ":" + idempotencyKey;
	}

	public boolean tryAcquire(String key, String requestHash) {
		try {
			IdempotencyRecord record = IdempotencyRecord.processing(requestHash);
			String json = objectMapper.writeValueAsString(record);
			Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, json, PROCESSING_TTL);
			return Boolean.TRUE.equals(acquired);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize IdempotencyRecord for key {}: {}", key, e.getMessage(), e);
			return false;
		}
	}

	public Optional<IdempotencyRecord> getRecord(String key) {
		String json = redisTemplate.opsForValue().get(key);
		if (json == null || json.isBlank()) {
			return Optional.empty();
		}
		try {
			IdempotencyRecord record = objectMapper.readValue(json, IdempotencyRecord.class);
			return Optional.of(record);
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize IdempotencyRecord for key {}: {}", key, e.getMessage(), e);
			return Optional.empty();
		}
	}

	public void markCompleted(String key, String requestHash, int statusCode, String responseBody) {
		try {
			IdempotencyRecord record = IdempotencyRecord.completed(requestHash, statusCode, responseBody);
			String json = objectMapper.writeValueAsString(record);
			redisTemplate.opsForValue().set(key, json, COMPLETED_TTL);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize IdempotencyRecord for completion on key {}: {}", key, e.getMessage(), e);
		}
	}

	public void release(String key) {
		redisTemplate.delete(key);
	}
}
