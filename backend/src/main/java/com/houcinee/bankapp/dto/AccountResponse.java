package com.houcinee.bankapp.dto;


import com.houcinee.bankapp.enums.Currency;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class AccountResponse {
	private UUID id;
	private String balance;
	private Currency currency;
	private UUID userId;
}
