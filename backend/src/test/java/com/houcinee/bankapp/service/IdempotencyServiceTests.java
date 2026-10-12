package com.houcinee.bankapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.houcinee.bankapp.dto.IdempotencyRecord;
import com.houcinee.bankapp.enums.IdempotencyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdempotencyServiceTests {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private ObjectMapper objectMapper;
	private IdempotencyService idempotencyService;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		idempotencyService = new IdempotencyService(redisTemplate, objectMapper);
	}

	@Test
	void givenValidUserIdAndKey_whenBuildKey_thenReturnsFormattedKey() {
		String userId = "user-123";
		String idempotencyKey = "req-abc-456";

		String key = idempotencyService.buildKey(userId, idempotencyKey);

		assertEquals("idempotency:user-123:req-abc-456", key);
	}

	@Test
	void givenNewKey_whenTryAcquire_thenReturnsTrue() {
		String key = "idempotency:user-1:key-1";
		String requestHash = "hash-123";
		when(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofSeconds(30))))
				.thenReturn(true);

		boolean acquired = idempotencyService.tryAcquire(key, requestHash);

		assertTrue(acquired);
		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).setIfAbsent(eq(key), jsonCaptor.capture(), eq(Duration.ofSeconds(30)));

		String capturedJson = jsonCaptor.getValue();
		assertNotNull(capturedJson);
		assertTrue(capturedJson.contains("\"status\":\"PROCESSING\""));
		assertTrue(capturedJson.contains("\"requestHash\":\"hash-123\""));
	}

	@Test
	void givenExistingKey_whenTryAcquire_thenReturnsFalse() {
		String key = "idempotency:user-1:key-1";
		String requestHash = "hash-123";
		when(valueOperations.setIfAbsent(eq(key), anyString(), eq(Duration.ofSeconds(30))))
				.thenReturn(false);

		boolean acquired = idempotencyService.tryAcquire(key, requestHash);

		assertFalse(acquired);
		verify(valueOperations).setIfAbsent(eq(key), anyString(), eq(Duration.ofSeconds(30)));
	}

	@Test
	void givenSerializationFails_whenTryAcquire_thenReturnsFalse() throws Exception {
		ObjectMapper mockMapper = mock(ObjectMapper.class);
		when(mockMapper.writeValueAsString(any()))
				.thenThrow(new JsonProcessingException("Serialization failed") {});
		IdempotencyService serviceWithMockMapper = new IdempotencyService(redisTemplate, mockMapper);

		boolean acquired = serviceWithMockMapper.tryAcquire("key", "hash");

		assertFalse(acquired);
	}

	@Test
	void givenMissingKey_whenGetRecord_thenReturnsEmpty() {
		String key = "idempotency:user-1:key-1";
		when(valueOperations.get(key)).thenReturn(null);

		Optional<IdempotencyRecord> result = idempotencyService.getRecord(key);

		assertTrue(result.isEmpty());
		verify(valueOperations).get(key);
	}

	@Test
	void givenBlankJson_whenGetRecord_thenReturnsEmpty() {
		String key = "idempotency:user-1:key-1";
		when(valueOperations.get(key)).thenReturn("   ");

		Optional<IdempotencyRecord> result = idempotencyService.getRecord(key);

		assertTrue(result.isEmpty());
	}

	@Test
	void givenValidJson_whenGetRecord_thenReturnsParsedRecord() throws Exception {
		String key = "idempotency:user-1:key-1";
		IdempotencyRecord original = IdempotencyRecord.completed("hash-xyz", 200, "{\"success\":true}");
		String json = objectMapper.writeValueAsString(original);
		when(valueOperations.get(key)).thenReturn(json);

		Optional<IdempotencyRecord> result = idempotencyService.getRecord(key);

		assertTrue(result.isPresent());
		IdempotencyRecord record = result.get();
		assertEquals(IdempotencyStatus.COMPLETED, record.status());
		assertEquals("hash-xyz", record.requestHash());
		assertEquals(200, record.statusCode());
		assertEquals("{\"success\":true}", record.responseBody());
	}

	@Test
	void givenInvalidJson_whenGetRecord_thenReturnsEmpty() {
		String key = "idempotency:user-1:key-1";
		when(valueOperations.get(key)).thenReturn("{invalid_json");

		Optional<IdempotencyRecord> result = idempotencyService.getRecord(key);

		assertTrue(result.isEmpty());
	}

	@Test
	void givenRecord_whenMarkCompleted_thenSavesWithTtl() {
		String key = "idempotency:user-1:key-1";
		String requestHash = "hash-123";
		int statusCode = 201;
		String responseBody = "{\"id\":\"acc-1\"}";

		idempotencyService.markCompleted(key, requestHash, statusCode, responseBody);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(eq(key), jsonCaptor.capture(), eq(Duration.ofHours(24)));

		String capturedJson = jsonCaptor.getValue();
		assertNotNull(capturedJson);
		assertTrue(capturedJson.contains("\"status\":\"COMPLETED\""));
		assertTrue(capturedJson.contains("\"requestHash\":\"hash-123\""));
		assertTrue(capturedJson.contains("\"statusCode\":201"));
		assertTrue(capturedJson.contains("acc-1"));
	}

	@Test
	void givenSerializationFails_whenMarkCompleted_thenLogsErrorWithoutThrowing() throws Exception {
		ObjectMapper mockMapper = mock(ObjectMapper.class);
		when(mockMapper.writeValueAsString(any()))
				.thenThrow(new JsonProcessingException("Serialization failed") {});
		IdempotencyService serviceWithMockMapper = new IdempotencyService(redisTemplate, mockMapper);

		assertDoesNotThrow(() -> serviceWithMockMapper.markCompleted("key", "hash", 200, "body"));
		verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
	}

	@Test
	void givenKey_whenRelease_thenDeletesFromRedis() {
		String key = "idempotency:user-1:key-1";

		idempotencyService.release(key);

		verify(redisTemplate).delete(key);
	}
}
