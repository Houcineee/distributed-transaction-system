package com.houcinee.bankapp.dto;

import com.houcinee.bankapp.enums.TransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class TransactionResponse {
	private UUID id;
	private String amount;
	private TransactionStatus status;
	private UUID sourceAccountId;
	private UUID destinationAccountId;
	private Instant createdAt;
}
