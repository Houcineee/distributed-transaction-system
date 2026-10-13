package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.dto.LoginRequest;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.enums.CardType;
import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.TransactionRepository;
import com.houcinee.bankapp.service.BaseIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class IdempotencyIT extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AccountRepository accountRepository;


	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final Map<String, String> redisStorage = new ConcurrentHashMap<>();

	@BeforeEach
	void setUpRedisStub() {
		redisStorage.clear();
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOps = mock(ValueOperations.class);
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

		when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(inv -> {
			String key = inv.getArgument(0);
			String val = inv.getArgument(1);
			return redisStorage.putIfAbsent(key, val) == null;
		});

		when(valueOps.get(anyString())).thenAnswer(inv -> {
			String key = inv.getArgument(0);
			return redisStorage.get(key);
		});

		doAnswer(inv -> {
			String key = inv.getArgument(0);
			String val = inv.getArgument(1);
			redisStorage.put(key, val);
			return null;
		}).when(valueOps).set(anyString(), anyString(), any(Duration.class));

		when(stringRedisTemplate.delete(anyString())).thenAnswer(inv -> {
			String key = inv.getArgument(0);
			return redisStorage.remove(key) != null;
		});
	}

	@AfterEach
	void tearDown() {
		jdbcTemplate.execute("TRUNCATE TABLE audit_logs, outbox_events, transactions, accounts, users CASCADE");
	}

	@Test
	void givenIdempotencyKey_whenTransferCalledTwice_thenDebitsOnlyOnceAndReturnsCachedResponse() throws Exception {
		// 1. Create Alice and Bob
		String userAId = createUser("Alice", "alice@bank.com");
		String userBId = createUser("Bob", "bob@bank.com");

		// 2. Log in
		String tokenA = login("alice@bank.com", "secure123");
		String tokenB = login("bob@bank.com", "secure123");

		// 3. Create Accounts
		String accountAId = createAccount(userAId, tokenA);
		String accountBId = createAccount(userBId, tokenB);

		// 4. Fund Alice's Account with $100.00 (10000 cents)
		fundAccount(accountAId, 10000L);

		TransactionRequest transferReq = TransactionRequest.builder()
				.srcAccount(UUID.fromString(accountAId))
				.destAccount(UUID.fromString(accountBId))
				.amount(4000L) // $40.00
				.build();

		// 5. First request: Alice transfers $40.00 to Bob with Idempotency-Key
		MvcResult firstResult = mockMvc.perform(post("/api/v1/transfers")
						.header("Idempotency-Key", "idemp-key-1")
						.cookie(new Cookie("jwt", tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.amount").value("40.00"))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andReturn();

		// Check DB: Alice balance is $60, Bob is $540, transaction count is 1
		Account aliceAccount = accountRepository.findById(UUID.fromString(accountAId)).orElseThrow();
		Account bobAccount = accountRepository.findById(UUID.fromString(accountBId)).orElseThrow();
		assertEquals(6000L, aliceAccount.getBalance());
		assertEquals(54000L, bobAccount.getBalance());
		assertEquals(1, transactionRepository.count());

		// 6. Second request with exact same Idempotency-Key and body
		MvcResult secondResult = mockMvc.perform(post("/api/v1/transfers")
						.header("Idempotency-Key", "idemp-key-1")
						.cookie(new Cookie("jwt", tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.amount").value("40.00"))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andReturn();

		// Verify cached JSON matches first response body
		assertEquals(firstResult.getResponse().getContentAsString(), secondResult.getResponse().getContentAsString());

		// Check DB: Balances and transaction count remain unchanged
		Account aliceAccountAfter = accountRepository.findById(UUID.fromString(accountAId)).orElseThrow();
		Account bobAccountAfter = accountRepository.findById(UUID.fromString(accountBId)).orElseThrow();
		assertEquals(6000L, aliceAccountAfter.getBalance());
		assertEquals(54000L, bobAccountAfter.getBalance());
		assertEquals(1, transactionRepository.count());
	}

	@Test
	void givenIdempotencyKey_whenSameKeySentWithDifferentAmount_thenReturns422UnprocessableEntity() throws Exception {
		// 1. Create Alice and Bob
		String userAId = createUser("Alice", "alice@bank.com");
		String userBId = createUser("Bob", "bob@bank.com");

		// 2. Log in
		String tokenA = login("alice@bank.com", "secure123");
		String tokenB = login("bob@bank.com", "secure123");

		// 3. Create Accounts
		String accountAId = createAccount(userAId, tokenA);
		String accountBId = createAccount(userBId, tokenB);

		// 4. Fund Alice's Account with $100.00 (10000 cents)
		fundAccount(accountAId, 10000L);

		TransactionRequest transferReq = TransactionRequest.builder()
				.srcAccount(UUID.fromString(accountAId))
				.destAccount(UUID.fromString(accountBId))
				.amount(4000L) // $40.00
				.build();

		// 5. First request: Alice transfers $40.00 to Bob with Idempotency-Key
		mockMvc.perform(post("/api/v1/transfers")
						.header("Idempotency-Key", "idemp-key-1")
						.cookie(new Cookie("jwt", tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.amount").value("40.00"))
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		// Check DB: Alice balance is $60, transaction count is 1
		Account aliceAccount = accountRepository.findById(UUID.fromString(accountAId)).orElseThrow();
		assertEquals(6000L, aliceAccount.getBalance());
		assertEquals(1, transactionRepository.count());

		// 6. Third request with same Idempotency-Key but different amount $80.00 (8000 cents)
		TransactionRequest differentReq = TransactionRequest.builder()
				.srcAccount(UUID.fromString(accountAId))
				.destAccount(UUID.fromString(accountBId))
				.amount(8000L) // $80.00
				.build();

		// Expect 422 Unprocessable Entity
		mockMvc.perform(post("/api/v1/transfers")
						.header("Idempotency-Key", "idemp-key-1")
						.cookie(new Cookie("jwt", tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(differentReq)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value("Idempotency key was previously used with different request parameters."));

		// Check DB: Balances and transaction count remain unchanged
		Account aliceAccountAfter = accountRepository.findById(UUID.fromString(accountAId)).orElseThrow();
		assertEquals(6000L, aliceAccountAfter.getBalance());
		assertEquals(1, transactionRepository.count());
	}

	// --- Helper Methods ---

	private String createUser(String name, String email) throws Exception {
		RegisterRequest req = RegisterRequest.builder()
				.name(name)
				.email(email)
				.password("secure123")
				.currency(Currency.MAD)
				.cardType(CardType.DEBIT)
				.build();
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String login(String email, String password) throws Exception {
		LoginRequest req = LoginRequest.builder()
				.email(email)
				.password(password)
				.build();
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andReturn();
		Cookie cookie = result.getResponse().getCookie("jwt");
		Assertions.assertNotNull(cookie, "JWT cookie should not be null");
		return cookie.getValue();
	}

	private String createAccount(String userId, String token) throws Exception {
		CreateAccountRequest req = CreateAccountRequest.builder()
				.userId(UUID.fromString(userId))
				.currency(Currency.MAD)
				.cardType(CardType.DEBIT)
				.build();
		MvcResult result = mockMvc.perform(post("/api/v1/accounts")
						.cookie(new Cookie("jwt", token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private void fundAccount(String accountId, Long balanceInCents) {
		Account account = accountRepository.findById(UUID.fromString(accountId)).orElseThrow();
		setField(account, "balance", balanceInCents);
		accountRepository.save(account);
	}
}
