package com.houcinee.bankapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.dto.event.TransferCompletedEvent;
import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.exception.TransferFailedException;
import com.houcinee.bankapp.helpers.MoneyUtil;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.OutboxEvent;
import com.houcinee.bankapp.model.Transaction;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.OutboxEventRepository;
import com.houcinee.bankapp.repository.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class TransactionService {
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	public TransactionService(AccountRepository accountRepository,
	                          TransactionRepository transactionRepository,
	                          OutboxEventRepository outboxEventRepository,
	                          ObjectMapper objectMapper) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.outboxEventRepository = outboxEventRepository;
		this.objectMapper = objectMapper;
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
			Transaction transaction = transactionRepository.save(new Transaction(amount, srcAccount, destAccount, TransactionStatus.COMPLETED));
			saveOutboxEvent(transaction, srcAccount.getId(), destAccount.getId(), amount);
			return toTransactionResponse(transaction);
		} catch (IllegalArgumentException e) {
			TransactionRequest reqContext = new TransactionRequest(sourceAccountId, destinationAccountId, amount);
			throw new TransferFailedException(e.getMessage(), reqContext);
		}
	}

	private void saveOutboxEvent(Transaction transaction, UUID sourceAccountId, UUID destinationAccountId, Long amount) {
		try {
			TransferCompletedEvent event = new TransferCompletedEvent(
				transaction.getId(),
				sourceAccountId,
				destinationAccountId,
				amount,
				Instant.now()
			);
			String payload = objectMapper.writeValueAsString(event);
			outboxEventRepository.save(new OutboxEvent("ACCOUNT", sourceAccountId, "TRANSFER_COMPLETED", payload));
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize TransferCompletedEvent for transaction {}", transaction.getId(), e);
			throw new RuntimeException("Failed to serialize outbox event", e);
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

		Account first = accountRepository.findByIdForUpdate(firstId)
		                                 .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + firstId));
		Account second = accountRepository.findByIdForUpdate(secondId)
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
