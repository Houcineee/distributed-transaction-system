package com.houcinee.bankapp.dto;

import com.houcinee.bankapp.enums.CardStatus;
import com.houcinee.bankapp.enums.CardType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Getter
public class CardResponse {
	private UUID id;
	private String cardNumber;
	private String cardholderName;
	private String cvv;
	private LocalDate expirationDate;
	private CardType cardType;
	private CardStatus status;
	private UUID accountId;
}
