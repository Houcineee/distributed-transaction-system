package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.exception.TransferFailedException;
import com.houcinee.bankapp.helpers.MoneyUtil;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.Transaction;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TransactionService {
	private final AccountRepository accountRepository;
	private final TransactionLoggerService transactionLoggerService;
	private final TransactionRepository transactionRepository;

	public TransactionService(AccountRepository accountRepository, TransactionLoggerService transactionLoggerService, TransactionRepository transactionRepository) {
		this.accountRepository = accountRepository;
		this.transactionLoggerService = transactionLoggerService;
		this.transactionRepository = transactionRepository;
	}

	@Transactional
	public TransactionResponse transferMoney(UUID sourceAccountId, UUID destinationAccountId, Long amount) {
		log.info("Initiating transfer of {} from account {} to account {}", amount, sourceAccountId, destinationAccountId);
		validateTransfer(sourceAccountId, destinationAccountId, amount);
		Account[] accounts = getAccountInLockOrder(sourceAccountId, destinationAccountId);

		Account srcAccount = sourceAccountId.equals(accounts[0].getId()) ? accounts[0] : accounts[1];
		Account destAccount = destinationAccountId.equals(accounts[0].getId()) ? accounts[0] : accounts[1];

		try {
			srcAccount.debit(amount);
			destAccount.credit(amount);
			Transaction transaction = transactionLoggerService.saveTransaction(amount, srcAccount, destAccount, TransactionStatus.COMPLETED);
			return toTransactionResponse(transaction);
		} catch (IllegalArgumentException e) {
			transactionLoggerService.saveTransaction(amount, srcAccount, destAccount, TransactionStatus.FAILED);
			
			// Context created only on failure to avoid unnecessary object allocation on happy path
			TransactionRequest reqContext = new TransactionRequest(sourceAccountId, destinationAccountId, amount);
			throw new TransferFailedException(e.getMessage(), reqContext);
		}
	}

	private void validateTransfer(UUID srcId, UUID destId, Long amount) {
		if (srcId.equals(destId)) {
			throw new TransferFailedException(
				"Source and destination accounts cannot be the same", 
				new TransactionRequest(srcId, destId, amount));
		}

		if (amount <= 0) {
			throw new TransferFailedException(
				"Transfer amount must be greater than zero", 
				new TransactionRequest(srcId, destId, amount));
		}
	}


	public Page<TransactionResponse> getTransactionsByAccountId(UUID id, Pageable pageable) {
		log.info("Fetching paginated transaction history for account id: {}", id);
		if (!accountRepository.existsById(id)) {
			throw new ResourceNotFoundException("Account not found with id: " + id);
		}
		Page<Transaction> transactionsPage = transactionRepository.findAccountTransactions(id, pageable);

		return transactionsPage.map(this::toTransactionResponse);
	}

	private Account[] getAccountInLockOrder(UUID id1, UUID id2) {
		UUID firstId = id1.compareTo(id2) < 0 ? id1 : id2;
		UUID secondId = id1.compareTo(id2) < 0 ? id2 : id1;

		Account first = accountRepository.findAccountById(firstId)
		                                 .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + firstId));
		Account second = accountRepository.findAccountById(secondId)
		                                  .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + secondId));

		return new Account[]{first, second};
	}


	private TransactionResponse toTransactionResponse(Transaction transaction) {
		return TransactionResponse.builder()
		                          .id(transaction.getId())
		                          .createdAt(transaction.getCreatedAt())
		                          .status(transaction.getStatus())
		                          .sourceAccountId(transaction.getSourceAccount()
		                                                      .getId())
		                          .destinationAccountId(transaction.getDestAccount()
		                                                           .getId())
		                          .amount(MoneyUtil.formatCents(transaction.getAmount()))
		                          .build();
	}
}
