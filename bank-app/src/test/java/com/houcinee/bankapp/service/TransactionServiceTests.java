package com.houcinee.bankapp.service;


import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.Transaction;
import com.houcinee.bankapp.model.Customer;
import com.houcinee.bankapp.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
	private TransactionLoggerService transactionLoggerService;

	@InjectMocks
	private TransactionService transactionService;

	private Account createAccount(long leastSigBits, long balance) {
		Customer customer = new Customer("Test Customer", "test@bank.com");
		ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());

		Account account = new Account(balance, Currency.MAD, customer);
		ReflectionTestUtils.setField(account, "id", new UUID(0L, leastSigBits));
		return account;
	}

	@Test
	void givenValidAccounts_whenTransferFunds_thenSuccessfulAndLogged() {
		Account srcAccount = createAccount(1L, 100L);
		Account destAccount = createAccount(2L, 50L);

		Transaction dummyTransaction = new Transaction(10L, srcAccount, destAccount, COMPLETED);
		ReflectionTestUtils.setField(dummyTransaction, "id", UUID.randomUUID());
		
		when(accountRepository.findAccountById(srcAccount.getId())).thenReturn(Optional.of(srcAccount));
		when(accountRepository.findAccountById(destAccount.getId())).thenReturn(Optional.of(destAccount));
		when(transactionLoggerService.saveTransaction(10L, srcAccount, destAccount, COMPLETED)).thenReturn(dummyTransaction);

		transactionService.transferMoney(srcAccount.getId(), destAccount.getId(), 10L);

		assertEquals(90L, srcAccount.getBalance());
		assertEquals(60L, destAccount.getBalance());

		verify(transactionLoggerService).saveTransaction(10L, srcAccount, destAccount, COMPLETED);
	}

	@Test
	void givenSameAccounts_whenTransferFunds_thenThrowsIllegalArgumentException() {
		Account srcAccount = createAccount(1L, 100L);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			transactionService.transferMoney(srcAccount.getId(), srcAccount.getId(), 20L);
		});

		assertEquals("Source and destination accounts cannot be the same", exception.getMessage());
		verify(accountRepository, never()).findAccountById(any());
	}

	@Test
	void givenNonPositiveAmount_whenTransferFunds_thenThrowsIllegalArgumentException() {
		Account srcAccount = createAccount(1L, 100L);
		Account destAccount = createAccount(2L, 50L);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			transactionService.transferMoney(srcAccount.getId(), destAccount.getId(), -10L);
		});

		assertEquals("Balance cannot be negative or zero", exception.getMessage());
	}

	@Test
	void givenInvalidAccountId_whenTransferFunds_thenThrowsResourceNotFoundException() {
		UUID srcUUID = new UUID(0L, 1L);
		UUID destUUID = new UUID(0L, 2L);

		when(accountRepository.findAccountById(srcUUID)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
			transactionService.transferMoney(srcUUID, destUUID, 10L);
		});

		assertEquals("Account not found" + srcUUID, exception.getMessage());
	}

	@Test
	void givenInsufficientFunds_whenTransferFunds_thenThrowsExceptionAndLogsFailed() {
		Account srcAccount = createAccount(1L, 5L);
		Account destAccount = createAccount(2L, 50L);

		when(accountRepository.findAccountById(srcAccount.getId())).thenReturn(Optional.of(srcAccount));
		when(accountRepository.findAccountById(destAccount.getId())).thenReturn(Optional.of(destAccount));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			transactionService.transferMoney(srcAccount.getId(), destAccount.getId(), 10L);
		});

		assertEquals("Insufficient balance in source account", exception.getMessage());

		verify(transactionLoggerService).saveTransaction(10L, srcAccount, destAccount, FAILED);
	}
}
