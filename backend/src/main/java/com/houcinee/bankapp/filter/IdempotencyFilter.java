package com.houcinee.bankapp.filter;

import com.houcinee.bankapp.dto.IdempotencyRecord;
import com.houcinee.bankapp.enums.IdempotencyStatus;
import com.houcinee.bankapp.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

	public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

	private final IdempotencyService idempotencyService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String method = request.getMethod();
		if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)) {
			return true;
		}
		String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
		return idempotencyKey == null || idempotencyKey.isBlank();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String userId = (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null)
				? auth.getName() : "anonymous";

		CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
		String body = new String(wrappedRequest.getCachedBody(), StandardCharsets.UTF_8);

		String requestHash = computeSha256(userId + ":" + request.getMethod() + ":" + request.getRequestURI() + ":" + body);
		String redisKey = idempotencyService.buildKey(userId, idempotencyKey);

		boolean acquired = idempotencyService.tryAcquire(redisKey, requestHash);
		if (!acquired) {
			Optional<IdempotencyRecord> optRecord = idempotencyService.getRecord(redisKey);
			if (optRecord.isPresent()) {
				IdempotencyRecord record = optRecord.get();
				if (record.status() == IdempotencyStatus.PROCESSING) {
					writeConflictResponse(response);
					return;
				}
				if (record.status() == IdempotencyStatus.COMPLETED) {
					if (!requestHash.equals(record.requestHash())) {
						writeUnprocessableEntityResponse(response);
						return;
					}
					writeCachedResponse(response, record);
					return;
				}
			}
			writeConflictResponse(response);
			return;
		}

		ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
		try {
			filterChain.doFilter(wrappedRequest, wrappedResponse);
			int status = wrappedResponse.getStatus();
			if (status < 500) {
				String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
				idempotencyService.markCompleted(redisKey, requestHash, status, responseBody);
			} else {
				idempotencyService.release(redisKey);
			}
		} catch (Exception e) {
			idempotencyService.release(redisKey);
			throw e;
		} finally {
			wrappedResponse.copyBodyToResponse();
		}
	}

	private void writeConflictResponse(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_CONFLICT);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"A request with this idempotency key is currently in progress. Please retry shortly.\"}");
	}

	private void writeUnprocessableEntityResponse(HttpServletResponse response) throws IOException {
		response.setStatus(422);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"Idempotency key was previously used with different request parameters.\"}");
	}

	private void writeCachedResponse(HttpServletResponse response, IdempotencyRecord record) throws IOException {
		response.setStatus(record.statusCode());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		if (record.responseBody() != null) {
			response.getWriter().write(record.responseBody());
		}
	}

	private String computeSha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(encodedHash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}
}
