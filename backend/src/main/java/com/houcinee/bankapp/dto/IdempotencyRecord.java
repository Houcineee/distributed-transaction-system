package com.houcinee.bankapp.dto;

import com.houcinee.bankapp.enums.IdempotencyStatus;

public record IdempotencyRecord(
	IdempotencyStatus status,
	String requestHash,
	Integer statusCode,
	String responseBody
) {
	public static IdempotencyRecord processing(String requestHash) {
		return new IdempotencyRecord(IdempotencyStatus.PROCESSING, requestHash, null, null);
	}

	public static IdempotencyRecord completed(String requestHash, int statusCode, String responseBody) {
		return new IdempotencyRecord(IdempotencyStatus.COMPLETED, requestHash, statusCode, responseBody);
	}
}
