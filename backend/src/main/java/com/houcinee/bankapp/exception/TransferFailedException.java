package com.houcinee.bankapp.exception;

import com.houcinee.bankapp.dto.TransactionRequest;
import lombok.Getter;

@Getter
public class TransferFailedException extends BankingException {

    private final TransactionRequest transactionRequest;

    public TransferFailedException(String message, TransactionRequest transactionRequest) {
        super(message);
        this.transactionRequest = transactionRequest;
    }
}
