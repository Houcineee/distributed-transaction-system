package com.houcinee.bankapp.dto;

import com.houcinee.bankapp.enums.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {
	@NotNull
	private UUID customerId ;
	@NotNull
	private Currency currency ;
}
