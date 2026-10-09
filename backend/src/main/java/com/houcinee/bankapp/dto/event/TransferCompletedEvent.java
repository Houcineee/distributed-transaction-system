package com.houcinee.bankapp.dto.event;

import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(
	UUID transactionId,
	UUID sourceAccountId,
	UUID destinationAccountId,
	Long amount,
	Instant timestamp
) {}
