package com.houcinee.bankapp.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.enums.Role;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.exception.TransferFailedException;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.model.OutboxEvent;
import com.houcinee.bankapp.model.Transaction;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.OutboxEventRepository;
import com.houcinee.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.houcinee.bankapp.enums.TransactionStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTests {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@org.mockito.Spy
	private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@InjectMocks
	private TransactionService transactionService;

	private Account createAccount(long leastSigBits, long balance) {
		AppUser appUser = new AppUser("Test AppUser", "test@bank.com", "password", Role.CUSTOMER);
		ReflectionTestUtils.setField(appUser, "id", UUID.randomUUID());

		Account account = new Account(balance, Currency.MAD, appUser);
		ReflectionTestUtils.setField(account, "id", new UUID(0L, leastSigBits));
		return account;
	}

	@Test
	void givenValidAccounts_whenTransferFunds_thenCompletesAndLogsTransaction() {
		Account srcAccount = createAccount(1L, 100L);
		Account destAccount = createAccount(2L, 50L);

		Transaction dummyTransaction = new Transaction(10L, srcAccount, destAccount, COMPLETED);
		ReflectionTestUtils.setField(dummyTransaction, "id", UUID.randomUUID());

		when(accountRepository.findByIdForUpdate(srcAccount.getId())).thenReturn(Optional.of(srcAccount));
		when(accountRepository.findByIdForUpdate(destAccount.getId())).thenReturn(Optional.of(destAccount));
		when(transactionRepository.save(any(Transaction.class))).thenReturn(dummyTransaction);

		transactionService.transferMoney(srcAccount.getId(), destAccount.getId(), 10L);

		assertEquals(90L, srcAccount.getBalance());
		assertEquals(60L, destAccount.getBalance());

		verify(transactionRepository).save(any(Transaction.class));
		verify(outboxEventRepository).save(any(OutboxEvent.class));
	}

	@Test
	void givenSameAccounts_whenTransferFunds_thenThrowsTransferFailedException() {
		Account srcAccount = createAccount(1L, 100L);

		TransferFailedException exception = assertThrows(TransferFailedException.class, () -> {
			transactionService.transferMoney(srcAccount.getId(), srcAccount.getId(), 20L);
		});

		assertEquals("Source and destination accounts cannot be the same", exception.getMessage());
		verify(accountRepository, never()).findByIdForUpdate(any());
		verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
	void givenNonPositiveAmount_whenTransferFunds_thenThrowsTransferFailedException() {
		Account srcAccount = createAccount(1L, 100L);
		Account destAccount = createAccount(2L, 50L);

		TransferFailedException exception = assertThrows(TransferFailedException.class, () -> {
			transactionService.transferMoney(srcAccount.getId(), destAccount.getId(), -10L);
		});

		assertEquals("Transfer amount must be greater than zero", exception.getMessage());
		verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
	void givenInvalidAccountId_whenTransferFunds_thenThrowsResourceNotFoundException() {
		UUID srcUUID = new UUID(0L, 1L);
		UUID destUUID = new UUID(0L, 2L);

		when(accountRepository.findByIdForUpdate(srcUUID)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
			transactionService.transferMoney(srcUUID, destUUID, 10L);
		});

		assertEquals("Account not found: " + srcUUID, exception.getMessage());
		verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
	void givenInsufficientFunds_whenTransferFunds_thenThrowsTransferFailedException() {
		Account srcAccount = createAccount(1L, 5L);
		Account destAccount = createAccount(2L, 50L);

		when(accountRepository.findByIdForUpdate(srcAccount.getId())).thenReturn(Optional.of(srcAccount));
		when(accountRepository.findByIdForUpdate(destAccount.getId())).thenReturn(Optional.of(destAccount));

		TransferFailedException exception = assertThrows(TransferFailedException.class, () -> {
			transactionService.transferMoney(srcAccount.getId(), destAccount.getId(), 10L);
		});

		assertEquals("Insufficient balance in source account", exception.getMessage());

		verify(transactionRepository, never()).save(any(Transaction.class));
		verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
	void givenReverseUUIDOrder_whenTransferFunds_thenCompletesSuccessfully() {
		// id1 > id2
		Account srcAccount = createAccount(2L, 100L); // 2L is greater than 1L
		Account destAccount = createAccount(1L, 50L);

		Transaction dummyTransaction = new Transaction(10L, srcAccount, destAccount, COMPLETED);
		ReflectionTestUtils.setField(dummyTransaction, "id", UUID.randomUUID());

		when(accountRepository.findByIdForUpdate(srcAccount.getId())).thenReturn(Optional.of(srcAccount));
		when(accountRepository.findByIdForUpdate(destAccount.getId())).thenReturn(Optional.of(destAccount));
		when(transactionRepository.save(any(Transaction.class))).thenReturn(dummyTransaction);

		transactionService.transferMoney(srcAccount.getId(), destAccount.getId(), 10L);

		assertEquals(90L, srcAccount.getBalance());
		assertEquals(60L, destAccount.getBalance());
		verify(transactionRepository).save(any(Transaction.class));
		verify(outboxEventRepository).save(any(OutboxEvent.class));
	}

	@Test
	void givenValidAccountId_whenGetTransactions_thenReturnsPaginatedResponses() {
		UUID accountId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(0, 10);
		
		Account srcAccount = createAccount(1L, 100L);
		Account destAccount = createAccount(2L, 50L);
		Transaction tx = new Transaction(10L, srcAccount, destAccount, COMPLETED);
		ReflectionTestUtils.setField(tx, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(tx, "createdAt", Instant.now());

		Page<Transaction> page = new PageImpl<>(List.of(tx));

		when(accountRepository.existsById(accountId)).thenReturn(true);
		when(transactionRepository.findAccountTransactions(accountId, pageable)).thenReturn(page);

		Page<TransactionResponse> responsePage = transactionService.getTransactionsByAccountId(accountId, pageable);

		assertEquals(1, responsePage.getTotalElements());
		assertEquals("0.10", responsePage.getContent().getFirst().getAmount());
	}

	@Test
	void givenInvalidAccountId_whenGetTransactions_thenThrowsResourceNotFoundException() {
		UUID accountId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(0, 10);

		when(accountRepository.existsById(accountId)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> {
			transactionService.getTransactionsByAccountId(accountId, pageable);
		});
	}
}
