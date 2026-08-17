package com.houcinee.bankapp.service;

import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class TransactionService {
	private final AccountRepository accountRepository;
	private final TransactionLoggerService transactionLoggerService;

	public TransactionService(AccountRepository accountRepository, TransactionLoggerService transactionLoggerService) {
		this.accountRepository = accountRepository;
		this.transactionLoggerService = transactionLoggerService;
	}

	@Transactional
	public void transferMoney(UUID sourceAccountId, UUID destinationAccountId, Long amount) {
		log.info("Initiating transfer of {} from account {} to account {}", amount, sourceAccountId, destinationAccountId);
		validateTransfer(sourceAccountId, destinationAccountId, amount);
		Account[] accounts = getAccountInLockOrder(sourceAccountId, destinationAccountId);

		Account srcAccount = sourceAccountId.equals(accounts[0].getId()) ? accounts[0] : accounts[1];
		Account destAccount = destinationAccountId.equals(accounts[0].getId()) ? accounts[0] : accounts[1];

		try {
			srcAccount.debit(amount);
			destAccount.credit(amount);
			transactionLoggerService.saveTransaction(amount, srcAccount, destAccount, TransactionStatus.COMPLETED);
		} catch (IllegalArgumentException e) {
			transactionLoggerService.saveTransaction(amount, srcAccount, destAccount, TransactionStatus.FAILED);
			throw e;
		}
	}

	private void validateTransfer(UUID srcId, UUID destId, Long amount) {
		if (srcId.equals(destId)) {
			throw new IllegalArgumentException("Source and destination accounts cannot be the same");
		}

		if (amount <= 0) {
			throw new IllegalArgumentException("Balance cannot be negative or zero");
		}
	}

	private Account[] getAccountInLockOrder(UUID id1, UUID id2) {
		UUID firstId = id1.compareTo(id2) < 0 ? id1 : id2;
		UUID secondId = id1.compareTo(id2) < 0 ? id2 : id1;

		Account first = accountRepository.findAccountById(firstId).orElseThrow(() -> new ResourceNotFoundException("Account not found" + firstId));
		Account second = accountRepository.findAccountById(secondId).orElseThrow(() -> new ResourceNotFoundException("Account not found" + secondId));

		return new Account[]{first, second};
	}
}
