package com.houcinee.bankapp.service;


import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.UserRepository;
import com.houcinee.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionServiceIT extends BaseIntegrationTest {
	@Autowired
	TransactionService transactionService;
	@Autowired
	AccountRepository accountRepository;
	@Autowired
	UserRepository userRepository;

	private UUID srcAccountId;
	private UUID destAccountId;

	@BeforeEach
	void setUp() {
		AppUser appUserA = userRepository.save(new AppUser("AppUser A", "user-a-" + UUID.randomUUID() + "@bank.com", "password", com.houcinee.bankapp.enums.Role.CUSTOMER));
		AppUser appUserB = userRepository.save(new AppUser("AppUser B", "user-b-" + UUID.randomUUID() + "@bank.com", "password", com.houcinee.bankapp.enums.Role.CUSTOMER));

		Account src = accountRepository.save(new Account(1000L, Currency.MAD, appUserA));
		Account dest = accountRepository.save(new Account(1000L, Currency.MAD, appUserB));

		srcAccountId = src.getId();
		destAccountId = dest.getId();
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void tearDown() {
		jdbcTemplate.execute("TRUNCATE TABLE audit_logs, outbox_events, transactions, accounts, users CASCADE");
	}

	@Test
	void givenConcurrentTransfers_whenExecuted_thenPreservesTotalBalance() throws InterruptedException {
		int threadCount = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch gateLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					gateLatch.await();
					transactionService.transferMoney(srcAccountId, destAccountId, 10L);
				} catch (Exception e) {
					throw new RuntimeException("Exception in worker thread: " + e.getMessage(), e);
				} finally {
					doneLatch.countDown();
				}
			});
		}
		gateLatch.countDown();
		doneLatch.await();
		executor.shutdown();


		Account src = accountRepository.findById(srcAccountId).orElseThrow();
		Account dest = accountRepository.findById(destAccountId).orElseThrow();

		assertEquals(500L, src.getBalance());
		assertEquals(1500L, dest.getBalance());
		assertEquals(2000L, src.getBalance() + dest.getBalance(), "Money must be conserved, total balance should not change");
	}
}
