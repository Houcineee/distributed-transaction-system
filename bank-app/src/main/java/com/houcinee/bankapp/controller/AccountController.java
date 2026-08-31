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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.parameters.P;

@RestController
public class AccountController {
	private final AccountService accountService;
	private final TransactionService transactionService;

	public AccountController(AccountService accountService , TransactionService transactionService) {
		this.accountService = accountService;
		this.transactionService = transactionService ;
	}

	@PostAuthorize("hasRole('ADMIN') or returnObject.body.userId == authentication.principal")
	@GetMapping("/api/v1/accounts/{id}")
	public ResponseEntity<AccountResponse> getAccountById(@PathVariable @P("id") UUID id) {
		return ResponseEntity.status(HttpStatus.OK).body(accountService.getAccountById(id));
	}

	@PreAuthorize("hasRole('ADMIN') or #req.userId == authentication.principal")
	@PostMapping("/api/v1/accounts")
	public ResponseEntity<AccountResponse> createAccount(@RequestBody @Valid @P("req") CreateAccountRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(req));
	}

	@PreAuthorize("hasRole('ADMIN') or @securityService.isAccountOwner(authentication, #id)")
	@GetMapping("/api/v1/accounts/{id}/transactions")
	public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(@PathVariable @P("id") UUID id , Pageable pageable){
		return ResponseEntity.status(HttpStatus.OK).body(transactionService.getTransactionsByAccountId(id , pageable)) ;
	}


}
