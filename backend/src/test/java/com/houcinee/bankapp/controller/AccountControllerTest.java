package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest extends BaseWebMvcTest {

	@MockitoBean
	private AccountService accountService;

	@MockitoBean
	private TransactionService transactionService;

	// --- createAccount Tests ---
	@Test
	void givenSameUser_whenCreateAccount_thenReturns201() throws Exception {
		UUID userId = UUID.randomUUID();
		CreateAccountRequest request = CreateAccountRequest.builder()
		                                                   .userId(userId)
		                                                   .currency(Currency.MAD)
		                                                   .cardType(com.houcinee.bankapp.enums.CardType.DEBIT)
		                                                   .build();

		AccountResponse response = AccountResponse.builder()
		                                          .id(UUID.randomUUID())
		                                          .balance("0.00")
		                                          .currency(Currency.MAD)
		                                          .userId(userId)
		                                          .build();

		when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/accounts").with(mockAuth(userId, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
		                                        .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isCreated())
		       .andExpect(jsonPath("$.balance").value("0.00"));
	}

	@Test
	void givenDifferentUser_whenCreateAccount_thenReturns403() throws Exception {
		UUID targetUserId = UUID.randomUUID();
		UUID loggedInUserId = UUID.randomUUID(); // Different ID!
		CreateAccountRequest request = CreateAccountRequest.builder()
		                                                   .userId(targetUserId)
		                                                   .currency(Currency.MAD)
		                                                   .cardType(com.houcinee.bankapp.enums.CardType.DEBIT)
		                                                   .build();

		mockMvc.perform(post("/api/v1/accounts").with(mockAuth(loggedInUserId, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
		                                        .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isForbidden());
	}

	@Test
	void givenInvalidRequest_whenCreateAccount_thenReturns400() throws Exception {
		UUID validUserId = UUID.randomUUID();
		// Missing currency
		CreateAccountRequest request = CreateAccountRequest.builder()
                                                           .userId(validUserId)
		                                                   .build();

		mockMvc.perform(post("/api/v1/accounts").with(mockAuth(validUserId, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
		                                        .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isBadRequest());
	}

	// --- getAccountById Tests ---
	@Test
	void givenSameUser_whenGetAccount_thenReturns200() throws Exception {
		UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
		AccountResponse response = AccountResponse.builder()
		                                          .id(accountId)
		                                          .balance("150.00")
		                                          .userId(userId) // user owns this account
		                                          .build();

		when(accountService.getAccountById(accountId)).thenReturn(response);

		mockMvc.perform(get("/api/v1/accounts/" + accountId).with(mockAuth(userId, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk());
	}

	@Test
	void givenDifferentUser_whenGetAccount_thenReturns403() throws Exception {
		UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
		UUID loggedInUserId = UUID.randomUUID(); // Different ID!
		AccountResponse response = AccountResponse.builder()
		                                          .id(accountId)
		                                          .balance("150.00")
		                                          .userId(ownerId)
		                                          .build();

		when(accountService.getAccountById(accountId)).thenReturn(response);

		mockMvc.perform(get("/api/v1/accounts/" + accountId).with(mockAuth(loggedInUserId, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isForbidden());
	}

	// --- getTransactionHistory Tests ---
	@Test
	void givenAccountOwner_whenGetTransactionHistory_thenReturns200() throws Exception {
		UUID accountId = UUID.randomUUID();
		Page<TransactionResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

		when(transactionService.getTransactionsByAccountId(eq(accountId), any())).thenReturn(page);
		// User owns the account
        when(securityService.isAccountOwner(any(), eq(accountId))).thenReturn(true);

		mockMvc.perform(get("/api/v1/accounts/" + accountId + "/transactions").param("page", "0")
		                                                                      .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
		                                                                      .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk());
	}

	@Test
	void givenNotAccountOwner_whenGetTransactionHistory_thenReturns403() throws Exception {
		UUID accountId = UUID.randomUUID();
		// User DOES NOT own the account
        when(securityService.isAccountOwner(any(), eq(accountId))).thenReturn(false);

		mockMvc.perform(get("/api/v1/accounts/" + accountId + "/transactions").param("page", "0")
		                                                                      .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
		                                                                      .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isForbidden());
	}

	// --- ADMIN Override Tests ---
	@Test
	void givenAdmin_whenGetAccount_thenReturns200() throws Exception {
		UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID(); // Some random user owns it
		AccountResponse response = AccountResponse.builder()
		                                          .id(accountId)
		                                          .balance("150.00")
		                                          .userId(ownerId)
		                                          .build();

		when(accountService.getAccountById(accountId)).thenReturn(response);

        // Admin does not own the account, but shouldn't matter!
		mockMvc.perform(get("/api/v1/accounts/" + accountId).with(mockAuth(UUID.randomUUID(), "ADMIN")).contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk());
	}

	@Test
	void givenAdmin_whenGetTransactionHistory_thenReturns200() throws Exception {
		UUID accountId = UUID.randomUUID();
		Page<TransactionResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

		when(transactionService.getTransactionsByAccountId(eq(accountId), any())).thenReturn(page);
		
        // Admin DOES NOT own the account (returns false), but should bypass it
        when(securityService.isAccountOwner(any(), eq(accountId))).thenReturn(false);

		mockMvc.perform(get("/api/v1/accounts/" + accountId + "/transactions").param("page", "0")
		                                                                      .with(mockAuth(UUID.randomUUID(), "ADMIN"))
		                                                                      .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk());
	}
}
