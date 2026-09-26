package com.houcinee.bankapp.service;

import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.Transaction;
import com.houcinee.bankapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionLoggerService {
	private final TransactionRepository transactionRepository;

	public TransactionLoggerService(TransactionRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Transaction saveTransaction(Long amount, Account srcAccount, Account destAccount, TransactionStatus status) {
		Transaction transaction = new Transaction(amount, srcAccount, destAccount, status);
		return transactionRepository.save(transaction);
	}
}
