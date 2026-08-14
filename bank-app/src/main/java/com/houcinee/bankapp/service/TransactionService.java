package com.houcinee.bankapp.service;

import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionService {
    private final AccountRepository accountRepository;
    private final   TransactionLoggerService transactionLoggerService ;

    public TransactionService(AccountRepository accountRepository, TransactionLoggerService transactionLoggerService) {
        this.accountRepository = accountRepository;
        this.transactionLoggerService = transactionLoggerService ;
    }

    @Transactional
    public void transferMoney(UUID sourceAccountId , UUID destinationAccountId , Long amount){
        Account srcAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account destAccount = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if(amount > srcAccount.getBalance()){
            transactionLoggerService.saveTransaction(amount , srcAccount , destAccount , TransactionStatus.FAILED); ;
            throw new IllegalArgumentException("Insufficient balance in source account");
        }
        srcAccount.setBalance(srcAccount.getBalance() - amount);
        destAccount.setBalance(destAccount.getBalance() + amount);
        accountRepository.save(srcAccount);
        accountRepository.save(destAccount);
        transactionLoggerService.saveTransaction(amount , srcAccount , destAccount , TransactionStatus.COMPLETED); ;

    }
}
