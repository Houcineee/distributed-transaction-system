package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.CardResponse;
import com.houcinee.bankapp.enums.CardType;
import com.houcinee.bankapp.service.AuditService;
import com.houcinee.bankapp.service.CardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@Slf4j
public class CardController {
	private final CardService cardService;
	private final AuditService auditService;

	public CardController(CardService cardService, AuditService auditService) {
		this.cardService = cardService;
		this.auditService = auditService;
	}

	@PreAuthorize("hasRole('ADMIN') or @securityService.isAccountOwner(authentication, #accountId)")
	@PostMapping
	public ResponseEntity<CardResponse> issueCard(
			@RequestParam @P("accountId") UUID accountId,
			@RequestParam(defaultValue = "DEBIT") CardType cardType,
			HttpServletRequest httpRequest) {
		CardResponse response = cardService.issueCard(accountId, cardType);
		auditService.recordCardCreated(response.getId(), response.getCardType().name(), accountId, httpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PreAuthorize("hasRole('ADMIN') or @securityService.isAccountOwner(authentication, #accountId)")
	@GetMapping("/account/{accountId}")
	public ResponseEntity<List<CardResponse>> getCardsByAccountId(@PathVariable @P("accountId") UUID accountId) {
		return ResponseEntity.status(HttpStatus.OK).body(cardService.getCardsByAccountId(accountId));
	}

	@PreAuthorize("hasRole('ADMIN') or @securityService.isCardOwner(authentication, #cardId)")
	@GetMapping("/{cardId}")
	public ResponseEntity<CardResponse> getCardById(@PathVariable @P("cardId") UUID cardId) {
		return ResponseEntity.status(HttpStatus.OK).body(cardService.getCardById(cardId));
	}

	@PreAuthorize("hasRole('ADMIN') or @securityService.isCardOwner(authentication, #cardId)")
	@PatchMapping("/{cardId}/block")
	public ResponseEntity<CardResponse> blockCard(@PathVariable @P("cardId") UUID cardId, HttpServletRequest httpRequest) {
		CardResponse response = cardService.blockCard(cardId);
		auditService.recordCardBlocked(cardId, httpRequest);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PreAuthorize("hasRole('ADMIN') or @securityService.isCardOwner(authentication, #cardId)")
	@PatchMapping("/{cardId}/activate")
	public ResponseEntity<CardResponse> activateCard(@PathVariable @P("cardId") UUID cardId, HttpServletRequest httpRequest) {
		CardResponse response = cardService.activateCard(cardId);
		auditService.recordCardActivated(cardId, httpRequest);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
