package com.houcinee.bankapp.model;

import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountTest {

	private AppUser dummyUser;

	@BeforeEach
	void setUp() {
		dummyUser = new AppUser("Test User", "test@domain.com", "password", Role.CUSTOMER);
	}

	// --- Debit Tests ---

	@Test
	void givenSufficientBalance_whenDebit_thenBalanceDecreases() {
		Account account = new Account(100L, Currency.MAD, dummyUser);

		account.debit(40L);

		assertEquals(60L, account.getBalance());
	}

	@Test
	void givenExactBalance_whenDebit_thenBalanceBecomesZero() {
		Account account = new Account(100L, Currency.MAD, dummyUser);

		account.debit(100L);

		assertEquals(0L, account.getBalance());
	}

	@Test
	void givenNegativeAmount_whenDebit_thenThrowsIllegalArgumentException() {
		Account account = new Account(100L, Currency.MAD, dummyUser);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			account.debit(-10L);
		});

		assertEquals("Transfer amount must be greater than zero", exception.getMessage());
	}

	@Test
	void givenZeroAmount_whenDebit_thenThrowsIllegalArgumentException() {
		Account account = new Account(100L, Currency.MAD, dummyUser);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			account.debit(0L);
		});

		assertEquals("Transfer amount must be greater than zero", exception.getMessage());
	}

	@Test
	void givenInsufficientBalance_whenDebit_thenThrowsIllegalArgumentException() {
		Account account = new Account(50L, Currency.MAD, dummyUser);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			account.debit(100L);
		});

		assertEquals("Insufficient balance in source account", exception.getMessage());
	}

	// --- Credit Tests ---

	@Test
	void givenValidAmount_whenCredit_thenBalanceIncreases() {
		Account account = new Account(100L, Currency.MAD, dummyUser);

		account.credit(50L);

		assertEquals(150L, account.getBalance());
	}

	@Test
	void givenNegativeAmount_whenCredit_thenThrowsIllegalArgumentException() {
		Account account = new Account(100L, Currency.MAD, dummyUser);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			account.credit(-20L);
		});

		assertEquals("Balance cannot be negative or zero", exception.getMessage());
	}

	@Test
	void givenZeroAmount_whenCredit_thenThrowsIllegalArgumentException() {
		Account account = new Account(100L, Currency.MAD, dummyUser);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			account.credit(0L);
		});

		assertEquals("Balance cannot be negative or zero", exception.getMessage());
	}
}
