package com.houcinee.bankapp.filter;

import com.houcinee.bankapp.dto.IdempotencyRecord;
import com.houcinee.bankapp.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdempotencyFilterTests {

	@Mock
	private IdempotencyService idempotencyService;

	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private IdempotencyFilter idempotencyFilter;

	@BeforeEach
	void setUp() {
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
				"user-123", null, Collections.emptyList());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private String computeHash(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(encodedHash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	@Test
	void givenNoIdempotencyKey_whenDoFilter_thenPassesThroughWithoutIdempotency() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/accounts");
		request.setContent("{\"currency\":\"USD\"}".getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		idempotencyFilter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(idempotencyService, never()).tryAcquire(anyString(), anyString());
		verify(idempotencyService, never()).getRecord(anyString());
	}

	@Test
	void givenGetRequestWithKey_whenDoFilter_thenPassesThroughWithoutIdempotency() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");
		request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, "key-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		idempotencyFilter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(idempotencyService, never()).tryAcquire(anyString(), anyString());
		verify(idempotencyService, never()).getRecord(anyString());
	}

	@Test
	void givenNewRequest_whenDoFilter_thenAcquiresLockAndMarksCompleted() throws ServletException, IOException {
		String userId = "user-123";
		String idempotencyKey = "key-abc";
		String uri = "/api/v1/transfers";
		String body = "{\"amount\":100}";
		String redisKey = "idempotency:" + userId + ":" + idempotencyKey;

		MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
		request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
		request.setContent(body.getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		when(idempotencyService.buildKey(userId, idempotencyKey)).thenReturn(redisKey);
		when(idempotencyService.tryAcquire(eq(redisKey), anyString())).thenReturn(true);

		doAnswer(invocation -> {
			HttpServletResponse resp = invocation.getArgument(1);
			resp.setStatus(HttpServletResponse.SC_CREATED);
			resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
			resp.getWriter().write("{\"id\":\"txn-1\",\"status\":\"SUCCESS\"}");
			return null;
		}).when(filterChain).doFilter(any(), any());

		idempotencyFilter.doFilter(request, response, filterChain);

		String expectedHash = computeHash(userId + ":POST:" + uri + ":" + body);
		verify(idempotencyService).tryAcquire(redisKey, expectedHash);
		verify(idempotencyService).markCompleted(redisKey, expectedHash, 201, "{\"id\":\"txn-1\",\"status\":\"SUCCESS\"}");
		assertEquals(201, response.getStatus());
		assertEquals("{\"id\":\"txn-1\",\"status\":\"SUCCESS\"}", response.getContentAsString());
	}

	@Test
	void givenConcurrentProcessingRequest_whenDoFilter_thenReturns409Conflict() throws ServletException, IOException {
		String userId = "user-123";
		String idempotencyKey = "key-abc";
		String redisKey = "idempotency:" + userId + ":" + idempotencyKey;

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
		request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
		request.setContent("{\"amount\":100}".getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		when(idempotencyService.buildKey(userId, idempotencyKey)).thenReturn(redisKey);
		when(idempotencyService.tryAcquire(eq(redisKey), anyString())).thenReturn(false);
		when(idempotencyService.getRecord(redisKey)).thenReturn(Optional.of(IdempotencyRecord.processing("some-hash")));

		idempotencyFilter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(any(), any());
		assertEquals(HttpServletResponse.SC_CONFLICT, response.getStatus());
		assertTrue(response.getContentAsString().contains("A request with this idempotency key is currently in progress. Please retry shortly."));
	}

	@Test
	void givenCompletedRequestSamePayload_whenDoFilter_thenReturnsCachedResponse() throws ServletException, IOException {
		String userId = "user-123";
		String idempotencyKey = "key-abc";
		String uri = "/api/v1/transfers";
		String body = "{\"amount\":100}";
		String redisKey = "idempotency:" + userId + ":" + idempotencyKey;
		String expectedHash = computeHash(userId + ":POST:" + uri + ":" + body);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
		request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
		request.setContent(body.getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		when(idempotencyService.buildKey(userId, idempotencyKey)).thenReturn(redisKey);
		when(idempotencyService.tryAcquire(eq(redisKey), eq(expectedHash))).thenReturn(false);
		when(idempotencyService.getRecord(redisKey)).thenReturn(
				Optional.of(IdempotencyRecord.completed(expectedHash, 201, "{\"id\":\"txn-1\",\"cached\":true}"))
		);

		idempotencyFilter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(any(), any());
		assertEquals(201, response.getStatus());
		assertEquals("{\"id\":\"txn-1\",\"cached\":true}", response.getContentAsString());
	}

	@Test
	void givenCompletedRequestDifferentPayload_whenDoFilter_thenReturns422UnprocessableEntity() throws ServletException, IOException {
		String userId = "user-123";
		String idempotencyKey = "key-abc";
		String uri = "/api/v1/transfers";
		String body = "{\"amount\":200}";
		String redisKey = "idempotency:" + userId + ":" + idempotencyKey;

		MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
		request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
		request.setContent(body.getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		when(idempotencyService.buildKey(userId, idempotencyKey)).thenReturn(redisKey);
		when(idempotencyService.tryAcquire(eq(redisKey), anyString())).thenReturn(false);
		when(idempotencyService.getRecord(redisKey)).thenReturn(
				Optional.of(IdempotencyRecord.completed("different-hash-value", 201, "{\"id\":\"txn-1\"}"))
		);

		idempotencyFilter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(any(), any());
		assertEquals(422, response.getStatus());
		assertTrue(response.getContentAsString().contains("Idempotency key was previously used with different request parameters."));
	}

	@Test
	void given500Error_whenDoFilter_thenReleasesLock() throws ServletException, IOException {
		String userId = "user-123";
		String idempotencyKey = "key-abc";
		String uri = "/api/v1/transfers";
		String body = "{\"amount\":100}";
		String redisKey = "idempotency:" + userId + ":" + idempotencyKey;

		MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
		request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
		request.setContent(body.getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		when(idempotencyService.buildKey(userId, idempotencyKey)).thenReturn(redisKey);
		when(idempotencyService.tryAcquire(eq(redisKey), anyString())).thenReturn(true);

		doAnswer(invocation -> {
			HttpServletResponse resp = invocation.getArgument(1);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}).when(filterChain).doFilter(any(), any());

		idempotencyFilter.doFilter(request, response, filterChain);

		verify(idempotencyService).release(redisKey);
		verify(idempotencyService, never()).markCompleted(anyString(), anyString(), anyInt(), anyString());
		assertEquals(500, response.getStatus());
	}

	@Test
	void givenExceptionThrown_whenDoFilter_thenReleasesLockAndRethrows() throws ServletException, IOException {
		String userId = "user-123";
		String idempotencyKey = "key-abc";
		String uri = "/api/v1/transfers";
		String body = "{\"amount\":100}";
		String redisKey = "idempotency:" + userId + ":" + idempotencyKey;

		MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
		request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
		request.setContent(body.getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		when(idempotencyService.buildKey(userId, idempotencyKey)).thenReturn(redisKey);
		when(idempotencyService.tryAcquire(eq(redisKey), anyString())).thenReturn(true);

		doThrow(new RuntimeException("Simulated error")).when(filterChain).doFilter(any(), any());

		assertThrows(RuntimeException.class, () -> idempotencyFilter.doFilter(request, response, filterChain));

		verify(idempotencyService).release(redisKey);
		verify(idempotencyService, never()).markCompleted(anyString(), anyString(), anyInt(), anyString());
	}

	@Test
	void givenMissingRecordWhenAcquireFails_whenDoFilter_thenReturns409Conflict() throws ServletException, IOException {
		String userId = "user-123";
		String idempotencyKey = "key-abc";
		String redisKey = "idempotency:" + userId + ":" + idempotencyKey;

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
		request.addHeader(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
		request.setContent("{\"amount\":100}".getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		when(idempotencyService.buildKey(userId, idempotencyKey)).thenReturn(redisKey);
		when(idempotencyService.tryAcquire(eq(redisKey), anyString())).thenReturn(false);
		when(idempotencyService.getRecord(redisKey)).thenReturn(Optional.empty());

		idempotencyFilter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(any(), any());
		assertEquals(HttpServletResponse.SC_CONFLICT, response.getStatus());
		assertTrue(response.getContentAsString().contains("A request with this idempotency key is currently in progress. Please retry shortly."));
	}

	@Nested
	class CachedBodyHttpServletRequestTests {

		@Test
		void givenRequestWithBody_whenReadMultipleTimes_thenYieldsSameBody() throws IOException {
			byte[] expectedBody = "{\"test\":\"value\"}".getBytes(StandardCharsets.UTF_8);
			MockHttpServletRequest rawRequest = new MockHttpServletRequest();
			rawRequest.setContent(expectedBody);

			CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(rawRequest);

			assertArrayEquals(expectedBody, cachedRequest.getCachedBody());

			ServletInputStream stream1 = cachedRequest.getInputStream();
			assertArrayEquals(expectedBody, stream1.readAllBytes());

			ServletInputStream stream2 = cachedRequest.getInputStream();
			assertArrayEquals(expectedBody, stream2.readAllBytes());

			try (BufferedReader reader = cachedRequest.getReader()) {
				assertEquals("{\"test\":\"value\"}", reader.readLine());
			}
		}
	}
}
