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

	@Test
	void givenValidRequest_whenCreateAccount_thenReturns201() throws Exception {
		CreateAccountRequest request = CreateAccountRequest.builder()
		                                                   .userId(UUID.randomUUID())
		                                                   .currency(Currency.MAD)
		                                                   .build();

		AccountResponse response = AccountResponse.builder()
		                                          .id(UUID.randomUUID())
		                                          .balance("0.00")
		                                          .currency(Currency.MAD)
		                                          .userId(request.getUserId())
		                                          .build();

		when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/accounts").with(mockAuth(request.getUserId(), "CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
		                                        .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isCreated())
		       .andExpect(jsonPath("$.balance").value("0.00"))
		       .andExpect(jsonPath("$.currency").value("MAD"));
	}

	@Test
	void givenInvalidRequest_whenCreateAccount_thenReturns400() throws Exception {
		// Missing currency, but valid userId so it passes security and hits @Valid
        UUID validUserId = UUID.randomUUID();
		CreateAccountRequest request = CreateAccountRequest.builder()
                                                           .userId(validUserId)
		                                                   .build();

		mockMvc.perform(post("/api/v1/accounts").with(mockAuth(validUserId, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
		                                        .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isBadRequest());
	}

	@Test
	void givenValidId_whenGetAccount_thenReturns200() throws Exception {
		UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
		AccountResponse response = AccountResponse.builder()
		                                          .id(accountId)
		                                          .balance("150.00")
		                                          .currency(Currency.USD)
		                                          .userId(userId)
		                                          .build();

		when(accountService.getAccountById(accountId)).thenReturn(response);

		mockMvc.perform(get("/api/v1/accounts/" + accountId).with(mockAuth(userId, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.balance").value("150.00"))
		       .andExpect(jsonPath("$.id").value(accountId.toString()));
	}

	@Test
	void givenValidId_whenGetTransactionHistory_thenReturnsPaginated200() throws Exception {
		UUID accountId = UUID.randomUUID();

		TransactionResponse tx = TransactionResponse.builder()
		                                            .id(UUID.randomUUID())
		                                            .amount("50.00")
		                                            .status(TransactionStatus.COMPLETED)
		                                            .createdAt(Instant.now())
		                                            .sourceAccountId(accountId)
		                                            .destinationAccountId(UUID.randomUUID())
		                                            .build();

		Page<TransactionResponse> page = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);

		when(transactionService.getTransactionsByAccountId(eq(accountId), any())).thenReturn(page);
        when(securityService.isAccountOwner(any(), eq(accountId))).thenReturn(true);

		mockMvc.perform(get("/api/v1/accounts/" + accountId + "/transactions").param("page", "0")
		                                                                      .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
		                                                                      .param("size", "20")
		                                                                      .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.content[0].amount").value("50.00"))
		       .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
		       .andExpect(jsonPath("$.totalElements").value(1))
		       .andExpect(jsonPath("$.totalPages").value(1));
	}
}
