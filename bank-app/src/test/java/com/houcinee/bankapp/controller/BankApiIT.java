package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.dto.LoginRequest;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.UserRepository;
import com.houcinee.bankapp.repository.TransactionRepository;
import com.houcinee.bankapp.service.BaseIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class BankApiIT extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@AfterEach
	void tearDown() {
		transactionRepository.deleteAllInBatch();
		accountRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	void givenFundedSourceAccount_whenTransfer_thenCompletesAndUpdatesBalances() throws Exception {
		// 1. Create AppUser A
		String userAId = createUser("Alice", "alice@bank.com");

		// 2. Create AppUser B
		String userBId = createUser("Bob", "bob@bank.com");

		// 3. Login to get token (using AppUser A's credentials)
		String tokenA = login("alice@bank.com", "secure123");
		
		// 4. Login to get token (using AppUser B's credentials)
		String tokenB = login("bob@bank.com", "secure123");

		// 5. Create Accounts (using respective tokens)
		String accountAId = createAccount(userAId, tokenA);
		String accountBId = createAccount(userBId, tokenB);

		// 6. Backdoor: Give Account A $100.00 (10000 cents) so they can transfer
		Account accountA = accountRepository.findById(UUID.fromString(accountAId))
		                                    .orElseThrow();
		setField(accountA, "balance", 10000L);
		accountRepository.save(accountA);

		// 7. Transfer $50.00 from A to B
		TransactionRequest transferReq = TransactionRequest.builder()
		                                                   .srcAccount(UUID.fromString(accountAId))
		                                                   .destAccount(UUID.fromString(accountBId))
		                                                   .amount(5000L) // $50.00
		                                                   .build();

		mockMvc.perform(post("/api/v1/transfers").header("Authorization", "Bearer " + tokenA)
		                                         .contentType(MediaType.APPLICATION_JSON)
		                                         .content(objectMapper.writeValueAsString(transferReq)))
		       .andExpect(status().isCreated())
		       .andExpect(jsonPath("$.amount").value("50.00"))
		       .andExpect(jsonPath("$.status").value("COMPLETED"));

		// 8. Verify Balances via API
		mockMvc.perform(get("/api/v1/accounts/" + accountAId).header("Authorization", "Bearer " + tokenA))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.balance").value("50.00"));

		mockMvc.perform(get("/api/v1/accounts/" + accountBId).header("Authorization", "Bearer " + tokenB))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.balance").value("50.00"));

		// 9. Verify Transaction History
		mockMvc.perform(get("/api/v1/accounts/" + accountAId + "/transactions?page=0&size=10").header("Authorization", "Bearer " + tokenA))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.totalElements").value(1))
		       .andExpect(jsonPath("$.content[0].amount").value("50.00"))
		       .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
	}

	@Test
	void givenInsufficientFunds_whenTransfer_thenRollsBackAndLogsFailure() throws Exception {
		// 1. Create users and accounts
		String userCId = createUser("Charlie", "charlie@bank.com");
		String userDId = createUser("Diana", "diana@bank.com");

		String tokenC = login("charlie@bank.com", "secure123");
		String tokenD = login("diana@bank.com", "secure123");

		String accountCId = createAccount(userCId, tokenC); // Balance starts at 0
		String accountDId = createAccount(userDId, tokenD); // Balance starts at 0

		// 2. Attempt to transfer $50.00 when balance is 0
		TransactionRequest transferReq = TransactionRequest.builder()
		                                                   .srcAccount(UUID.fromString(accountCId))
		                                                   .destAccount(UUID.fromString(accountDId))
		                                                   .amount(5000L) // $50.00
		                                                   .build();

		mockMvc.perform(post("/api/v1/transfers").header("Authorization", "Bearer " + tokenC)
		                                         .contentType(MediaType.APPLICATION_JSON)
		                                         .content(objectMapper.writeValueAsString(transferReq)))
		       .andExpect(status().isBadRequest())
		       .andExpect(jsonPath("$.message").value("Insufficient balance in source account"));

		// 3. Verify Balances remained 0 (Rolled back)
		mockMvc.perform(get("/api/v1/accounts/" + accountCId).header("Authorization", "Bearer " + tokenC))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.balance").value("0.00"));

		// 4. Verify FAILED transaction was logged
		mockMvc.perform(get("/api/v1/accounts/" + accountCId + "/transactions").header("Authorization", "Bearer " + tokenC))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.totalElements").value(1))
		       .andExpect(jsonPath("$.content[0].status").value("FAILED"));
	}

	@Test
	void givenRegistrationRequest_whenRegister_thenStoresHashedPassword() throws Exception {
		String rawPassword = "superSecretPassword!";
		RegisterRequest req = RegisterRequest.builder()
		                                     .name("Eve")
		                                     .email("eve@hacker.com")
		                                     .password(rawPassword)
		                                     .build();

		MvcResult result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
		                                                                .content(objectMapper.writeValueAsString(req)))
		                          .andExpect(status().isCreated())
		                          .andReturn();

		String userIdStr = JsonPath.read(result.getResponse()
		                                           .getContentAsString(), "$.id");
		UUID userId = UUID.fromString(userIdStr);

		AppUser savedAppUser = userRepository.findById(userId)
		                                     .orElseThrow();

		// Assert that the database value is NOT the raw password
		org.junit.jupiter.api.Assertions.assertNotEquals(rawPassword, savedAppUser.getPassword());

		// Bcrypt hashes start with $2a$, $2b$, or $2y$
		org.junit.jupiter.api.Assertions.assertTrue(savedAppUser.getPassword()
		                                                        .startsWith("$2a$"));
	}

	// --- Helpers ---
	private String createUser(String name, String email) throws Exception {
		RegisterRequest req = RegisterRequest.builder()
		                                     .name(name)
		                                     .email(email)
		                                     .password("secure123")
		                                     .build();
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
		                                                                .content(objectMapper.writeValueAsString(req)))
		                          .andExpect(status().isCreated())
		                          .andReturn();
		return JsonPath.read(result.getResponse()
		                           .getContentAsString(), "$.id");
	}

	private String login(String email, String password) throws Exception {
		LoginRequest req = LoginRequest.builder()
		                               .email(email)
		                               .password(password)
		                               .build();
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
		                                                             .content(objectMapper.writeValueAsString(req)))
		                          .andExpect(status().isOk())
		                          .andReturn();
		return JsonPath.read(result.getResponse()
		                           .getContentAsString(), "$.token");
	}

	private String createAccount(String userId, String token) throws Exception {
		CreateAccountRequest req = CreateAccountRequest.builder()
		                                               .userId(UUID.fromString(userId))
		                                               .currency(Currency.MAD)
		                                               .build();
		MvcResult result = mockMvc.perform(post("/api/v1/accounts").header("Authorization", "Bearer " + token)
		                                                           .contentType(MediaType.APPLICATION_JSON)
		                                                           .content(objectMapper.writeValueAsString(req)))
		                          .andExpect(status().isCreated())
		                          .andReturn();
		return JsonPath.read(result.getResponse()
		                           .getContentAsString(), "$.id");
	}
}
