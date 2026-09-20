package com.houcinee.bankapp.service;

import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.model.Transaction;
import com.houcinee.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionLoggerServiceTests {

	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private TransactionLoggerService transactionLoggerService;

	@Captor
	private ArgumentCaptor<Transaction> transactionCaptor;

	@Test
	void givenValidDetails_whenSaveTransaction_thenSavesAndReturnsTransaction() {
		// Arrange
		AppUser dummyUser = new AppUser();
		Account srcAccount = new Account(1000L, Currency.USD, dummyUser);
		Account destAccount = new Account(500L, Currency.USD, dummyUser);
		Long amount = 200L;
		TransactionStatus status = TransactionStatus.COMPLETED;

		Transaction savedTransaction = new Transaction(amount, srcAccount, destAccount, status);
		when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

		// Act
		Transaction result = transactionLoggerService.saveTransaction(amount, srcAccount, destAccount, status);

		// Assert
		assertThat(result).isNotNull();
		
		verify(transactionRepository).save(transactionCaptor.capture());
		Transaction capturedTransaction = transactionCaptor.getValue();

		assertThat(capturedTransaction.getAmount()).isEqualTo(amount);
		assertThat(capturedTransaction.getSourceAccount()).isEqualTo(srcAccount);
		assertThat(capturedTransaction.getDestAccount()).isEqualTo(destAccount);
		assertThat(capturedTransaction.getStatus()).isEqualTo(status);
	}
}
