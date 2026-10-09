package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.dto.LoginRequest;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.enums.AuditAction;
import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.model.AuditLog;
import com.houcinee.bankapp.model.OutboxEvent;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.AuditLogRepository;
import com.houcinee.bankapp.repository.OutboxEventRepository;
import com.houcinee.bankapp.repository.UserRepository;
import com.houcinee.bankapp.repository.TransactionRepository;
import com.houcinee.bankapp.service.BaseIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class BankApiIT extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UserRepository userRepository;


	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void tearDown() {
		jdbcTemplate.execute("TRUNCATE TABLE audit_logs, outbox_events, transactions, accounts, users CASCADE");
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

		mockMvc.perform(post("/api/v1/transfers").cookie(new Cookie("jwt", tokenA))
		                                         .contentType(MediaType.APPLICATION_JSON)
		                                         .content(objectMapper.writeValueAsString(transferReq)))
		       .andExpect(status().isCreated())
		       .andExpect(jsonPath("$.amount").value("50.00"))
		       .andExpect(jsonPath("$.status").value("COMPLETED"));

		// 8. Verify Balances via API
		mockMvc.perform(get("/api/v1/accounts/" + accountAId).cookie(new Cookie("jwt", tokenA)))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.balance").value("50.00"));

		mockMvc.perform(get("/api/v1/accounts/" + accountBId).cookie(new Cookie("jwt", tokenB)))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.balance").value("50.00"));

		// 9. Verify Transaction History
		mockMvc.perform(get("/api/v1/accounts/" + accountAId + "/transactions?page=0&size=10").cookie(new Cookie("jwt", tokenA)))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.totalElements").value(1))
		       .andExpect(jsonPath("$.content[0].amount").value("50.00"))
		       .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));

		// 10. Verify Audit Log was recorded in the database
		List<AuditLog> auditLogs = auditLogRepository.findAll();
		boolean transferAuditExists = auditLogs.stream()
		                                       .anyMatch(l -> l.getAction() == AuditAction.TRANSFER_EXECUTED && "TRANSACTION".equals(l.getTargetEntityType()) && l.getDetails()
		                                                                                                                                                          .contains("50.00"));
		assertTrue(transferAuditExists, "Audit log for TRANSFER_EXECUTED should be present in database");

		// 11. Verify Outbox Event was recorded in PostgreSQL
		List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
		assertEquals(1, outboxEvents.size(), "Exactly 1 outbox event should be recorded");
		OutboxEvent outboxEvent = outboxEvents.getFirst();
		assertEquals("ACCOUNT", outboxEvent.getAggregateType());
		assertEquals(UUID.fromString(accountAId), outboxEvent.getAggregateId());
		assertEquals("TRANSFER_COMPLETED", outboxEvent.getEventType());
		assertTrue(outboxEvent.getPayload().contains("5000"), "Outbox event payload should contain transfer amount");
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

		mockMvc.perform(post("/api/v1/transfers").cookie(new Cookie("jwt", tokenC))
		                                         .contentType(MediaType.APPLICATION_JSON)
		                                         .content(objectMapper.writeValueAsString(transferReq)))
		       .andExpect(status().isBadRequest())
		       .andExpect(jsonPath("$.message").value("Insufficient balance in source account"));

		// 3. Verify Balances remained 0 (Rolled back)
		mockMvc.perform(get("/api/v1/accounts/" + accountCId).cookie(new Cookie("jwt", tokenC)))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.balance").value("0.00"));

		// 4. Verify NO transaction ledger entry was committed (Atomic Rollback)
		mockMvc.perform(get("/api/v1/accounts/" + accountCId + "/transactions").cookie(new Cookie("jwt", tokenC)))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.totalElements").value(0));

		// 5. Verify TRANSFER_FAILED audit log was recorded in the database
		List<AuditLog> failedLogs = auditLogRepository.findAll();
		boolean transferFailedAuditExists = failedLogs.stream()
		                                              .anyMatch(l -> l.getAction() == AuditAction.TRANSFER_FAILED && l.getDetails()
		                                                                                                              .contains("Insufficient balance in source account"));
		assertTrue(transferFailedAuditExists, "Audit log for TRANSFER_FAILED should be present in database");

		// 6. Verify NO outbox event was committed (Atomic Rollback)
		List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
		assertTrue(outboxEvents.isEmpty(), "Outbox table should remain empty on rolled-back transfer");
	}

	@Test
	void givenRegistrationRequest_whenRegister_thenStoresHashedPassword() throws Exception {
		String rawPassword = "superSecretPassword!";
		RegisterRequest req = RegisterRequest.builder()
		                                     .name("Eve")
		                                     .email("eve@hacker.com")
		                                     .password(rawPassword)
		                                     .currency(Currency.MAD)
		                                     .cardType(com.houcinee.bankapp.enums.CardType.DEBIT)
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
		assertNotEquals(rawPassword, savedAppUser.getPassword());

		// Bcrypt hashes start with $2a$, $2b$, or $2y$
		assertTrue(savedAppUser.getPassword()
		                       .startsWith("$2a$"));
	}

	@Test
	void givenImmutableAuditLog_whenAttemptDelete_thenDatabaseRejects() {
		// 1. Manually create and save an audit log
		AuditLog log = AuditLog.builder()
		                       .action(AuditAction.LOGIN_SUCCESS)
		                       .ipAddress("127.0.0.1")
		                       .correlationId(UUID.randomUUID().toString())
		                       .details("{}")
		                       .build();
		log = auditLogRepository.saveAndFlush(log);

		final AuditLog savedLog = log;

		// 2. Attempt to delete it. The DB trigger should block this and throw a JpaSystemException
		JpaSystemException exception = Assertions.assertThrows(
				JpaSystemException.class,
				() -> {
					auditLogRepository.delete(savedLog);
					auditLogRepository.flush(); // Force Hibernate to execute the DELETE immediately
				}
		);

		// 3. Verify the exception message comes from our Postgres trigger
		assertTrue(exception.getCause().getCause().getMessage().contains("Compliance Violation"));
	}

	@Test
	void givenAccountCreation_whenSuccess_thenAutoIssuesDebitCard() throws Exception {
		// 1. Create users and accounts
		String userId = createUser("Bob", "bob@bank.com");
		String token = login("bob@bank.com", "secure123");

		String accountId = createAccount(userId, token); 

		// 2. Fetch cards for account
		mockMvc.perform(get("/api/v1/cards/account/" + accountId).cookie(new Cookie("jwt", token)))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.length()").value(1))
		       .andExpect(jsonPath("$[0].cardType").value("DEBIT"))
		       // Validate masking over the wire
		       .andExpect(jsonPath("$[0].cardNumber").value(org.hamcrest.Matchers.startsWith("**** **** **** ")))
		       .andExpect(jsonPath("$[0].cvv").value("***"));
	}

	// --- Helpers ---
	private String createUser(String name, String email) throws Exception {
		RegisterRequest req = RegisterRequest.builder()
		                                     .name(name)
		                                     .email(email)
		                                     .password("secure123")
		                                     .currency(Currency.MAD)
		                                     .cardType(com.houcinee.bankapp.enums.CardType.DEBIT)
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
		Cookie cookie = result.getResponse().getCookie("jwt");
		Assertions.assertNotNull(cookie, "JWT cookie should not be null");
		return cookie.getValue();
	}

	private String createAccount(String userId, String token) throws Exception {
		CreateAccountRequest req = CreateAccountRequest.builder()
		                                               .userId(UUID.fromString(userId))
		                                               .currency(Currency.MAD)
		                                               .cardType(com.houcinee.bankapp.enums.CardType.DEBIT)
		                                               .build();
		MvcResult result = mockMvc.perform(post("/api/v1/accounts").cookie(new Cookie("jwt", token))
		                                                           .contentType(MediaType.APPLICATION_JSON)
		                                                           .content(objectMapper.writeValueAsString(req)))
		                          .andExpect(status().isCreated())
		                          .andReturn();
		return JsonPath.read(result.getResponse()
		                           .getContentAsString(), "$.id");
	}
}
