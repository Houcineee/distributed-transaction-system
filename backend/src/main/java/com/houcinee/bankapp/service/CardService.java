package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.CardResponse;
import com.houcinee.bankapp.enums.CardStatus;
import com.houcinee.bankapp.enums.CardType;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.Card;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.CardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class CardService {
	private final CardRepository cardRepository;
	private final AccountRepository accountRepository;
	private final Random random = new Random();

	private static final String BIN_PREFIX = "453900";

	public CardService(CardRepository cardRepository, AccountRepository accountRepository) {
		this.cardRepository = cardRepository;
		this.accountRepository = accountRepository;
	}

	@Transactional
	public CardResponse issueCard(UUID accountId, CardType cardType) {
		log.info("Issuing {} card for account: {}", cardType, accountId);

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

		String cardNumber = generateLuhnValidCardNumber();
		String cvv = generateCvv();
		LocalDate expirationDate = LocalDate.now().plusYears(3);

		Card card = new Card(
				cardNumber,
				account.getAppUser().getName(),
				cvv,
				expirationDate,
				cardType,
				account
		);

		cardRepository.save(card);

		log.info("Successfully issued card {} for account {}", card.getId(), accountId);
		return toCardResponse(card);
	}

	public List<CardResponse> getCardsByAccountId(UUID accountId) {
		log.info("Fetching all cards for account id: {}", accountId);
		if (!accountRepository.existsById(accountId)) {
			throw new ResourceNotFoundException("Account not found with id: " + accountId);
		}
		return cardRepository.findAllByAccountId(accountId).stream()
				.map(this::toMaskedCardResponse)
				.toList();
	}

	public CardResponse getCardById(UUID cardId) {
		log.info("Fetching card details for id: {}", cardId);
		Card card = cardRepository.findById(cardId)
				.orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
		return toMaskedCardResponse(card);
	}

	@Transactional
	public CardResponse blockCard(UUID cardId) {
		log.info("Blocking card: {}", cardId);
		Card card = cardRepository.findById(cardId)
				.orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
		card.block();
		cardRepository.save(card);
		log.info("Successfully blocked card {}", cardId);
		return toMaskedCardResponse(card);
	}

	@Transactional
	public CardResponse activateCard(UUID cardId) {
		log.info("Activating card: {}", cardId);
		Card card = cardRepository.findById(cardId)
				.orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
		card.activate();
		cardRepository.save(card);
		log.info("Successfully activated card {}", cardId);
		return toMaskedCardResponse(card);
	}

	// ===== Card Number Generation (Luhn Algorithm) =====

	String generateLuhnValidCardNumber() {
		String partial = BIN_PREFIX + generateRandomDigits(9);
		int checkDigit = computeLuhnCheckDigit(partial);
		String cardNumber = partial + checkDigit;

		// Extremely unlikely collision, but the DB unique constraint is the real guard
		if (cardRepository.existsByCardNumber(cardNumber)) {
			log.warn("Card number collision detected, regenerating...");
			return generateLuhnValidCardNumber();
		}

		return cardNumber;
	}

	private int computeLuhnCheckDigit(String partial) {
		int sum = 0;
		for (int i = 0; i < partial.length(); i++) {
			int digit = Character.getNumericValue(partial.charAt(i));

			// Double every second digit from the right (since we're iterating left-to-right
			// and the check digit will be appended, digits at even indices get doubled)
			if (i % 2 == 0) {
				digit *= 2;
				if (digit > 9) {
					digit -= 9;
				}
			}
			sum += digit;
		}
		return (10 - (sum % 10)) % 10;
	}

	private String generateRandomDigits(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(random.nextInt(10));
		}
		return sb.toString();
	}

	private String generateCvv() {
		return String.format("%03d", random.nextInt(1000));
	}

	// ===== Mapper =====

	private CardResponse toCardResponse(Card card) {
		return CardResponse.builder()
				.id(card.getId())
				.cardNumber(card.getCardNumber())
				.cardholderName(card.getCardholderName())
				.cvv(card.getCvv())
				.expirationDate(card.getExpirationDate())
				.cardType(card.getCardType())
				.status(card.getStatus())
				.accountId(card.getAccount().getId())
				.build();
	}

	private CardResponse toMaskedCardResponse(Card card) {
		String maskedNumber = "**** **** **** " + card.getCardNumber().substring(12);
		return CardResponse.builder()
				.id(card.getId())
				.cardNumber(maskedNumber)
				.cardholderName(card.getCardholderName())
				.cvv("***")
				.expirationDate(card.getExpirationDate())
				.cardType(card.getCardType())
				.status(card.getStatus())
				.accountId(card.getAccount().getId())
				.build();
	}
}
