package com.houcinee.bankapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TransactionRequest {
	@NotNull
	private UUID srcAccount ;
	@NotNull
	private UUID destAccount ;
	@NotNull
	@Positive
	private Long balance ;
}
