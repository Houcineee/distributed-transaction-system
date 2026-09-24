package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.CardResponse;
import com.houcinee.bankapp.enums.CardStatus;
import com.houcinee.bankapp.enums.CardType;
import com.houcinee.bankapp.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(CardController.class)
class CardControllerTest extends BaseWebMvcTest {

	@MockitoBean
	private CardService cardService;

	// --- issueCard Tests ---
	@Test
	void givenAccountOwner_whenIssueCard_thenReturns201() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID loggedInUser = UUID.randomUUID();
		CardResponse response = CardResponse.builder()
				.id(UUID.randomUUID())
				.cardNumber("4539001234567890")
				.cvv("123")
				.cardType(CardType.DEBIT)
				.accountId(accountId)
				.build();

		when(securityService.isAccountOwner(any(), eq(accountId))).thenReturn(true);
		when(cardService.issueCard(eq(accountId), any(CardType.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/cards")
						.param("accountId", accountId.toString())
						.param("cardType", "DEBIT")
						.with(mockAuth(loggedInUser, "CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cardNumber").value("4539001234567890"));

		verify(auditService).recordCardCreated(eq(response.getId()), eq("DEBIT"), eq(accountId), any());
	}

	@Test
	void givenNotAccountOwner_whenIssueCard_thenReturns403() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(securityService.isAccountOwner(any(), eq(accountId))).thenReturn(false);

		mockMvc.perform(post("/api/v1/cards")
						.param("accountId", accountId.toString())
						.param("cardType", "DEBIT")
						.with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	// --- getCardsByAccountId Tests ---
	@Test
	void givenAccountOwner_whenGetCardsByAccountId_thenReturns200() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(securityService.isAccountOwner(any(), eq(accountId))).thenReturn(true);
		when(cardService.getCardsByAccountId(accountId)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/cards/account/" + accountId)
						.with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	void givenNotAccountOwner_whenGetCardsByAccountId_thenReturns403() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(securityService.isAccountOwner(any(), eq(accountId))).thenReturn(false);

		mockMvc.perform(get("/api/v1/cards/account/" + accountId)
						.with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	// --- getCardById Tests ---
	@Test
	void givenCardOwner_whenGetCardById_thenReturns200() throws Exception {
		UUID cardId = UUID.randomUUID();
		CardResponse response = CardResponse.builder().id(cardId).build();

		when(securityService.isCardOwner(any(), eq(cardId))).thenReturn(true);
		when(cardService.getCardById(cardId)).thenReturn(response);

		mockMvc.perform(get("/api/v1/cards/" + cardId)
						.with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	void givenNotCardOwner_whenGetCardById_thenReturns403() throws Exception {
		UUID cardId = UUID.randomUUID();
		when(securityService.isCardOwner(any(), eq(cardId))).thenReturn(false);

		mockMvc.perform(get("/api/v1/cards/" + cardId)
						.with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	// --- blockCard / activateCard Tests ---
	@Test
	void givenCardOwner_whenBlockCard_thenReturns200() throws Exception {
		UUID cardId = UUID.randomUUID();
		CardResponse response = CardResponse.builder().id(cardId).status(CardStatus.BLOCKED).build();

		when(securityService.isCardOwner(any(), eq(cardId))).thenReturn(true);
		when(cardService.blockCard(cardId)).thenReturn(response);

		mockMvc.perform(patch("/api/v1/cards/" + cardId + "/block")
						.with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		verify(auditService).recordCardBlocked(eq(cardId), any());
	}
	
	@Test
	void givenAdmin_whenBlockCard_thenReturns200() throws Exception {
		UUID cardId = UUID.randomUUID();
		CardResponse response = CardResponse.builder().id(cardId).status(CardStatus.BLOCKED).build();

		when(securityService.isCardOwner(any(), eq(cardId))).thenReturn(false); // Not owner
		when(cardService.blockCard(cardId)).thenReturn(response);

		mockMvc.perform(patch("/api/v1/cards/" + cardId + "/block")
						.with(mockAuth(UUID.randomUUID(), "ADMIN")) // Is Admin
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}
}
