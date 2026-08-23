package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@RestController
public class AccountController {
	private final AccountService accountService;
	private final TransactionService transactionService;

	public AccountController(AccountService accountService , TransactionService transactionService) {
		this.accountService = accountService;
		this.transactionService = transactionService ;
	}

	@GetMapping("/api/v1/accounts/{id}")
	public ResponseEntity<AccountResponse> getAccountById(@PathVariable UUID id) {
		return ResponseEntity.status(HttpStatus.OK).body(accountService.getAccountById(id));
	}

	@PostMapping("/api/v1/accounts")
	public ResponseEntity<AccountResponse> createAccount(@RequestBody @Valid CreateAccountRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(req));
	}

	@GetMapping("/api/v1/accounts/{id}/transactions")
	public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(@PathVariable UUID id , Pageable pageable){
		return ResponseEntity.status(HttpStatus.OK).body(transactionService.getTransactionsByAccountId(id , pageable)) ;
	}


}
