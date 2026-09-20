package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import com.houcinee.bankapp.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
public class AccountController {
	private final AccountService accountService;
	private final TransactionService transactionService;
	private final AuditService auditService;

	public AccountController(AccountService accountService, TransactionService transactionService, AuditService auditService) {
		this.accountService = accountService;
		this.transactionService = transactionService;
		this.auditService = auditService;
	}

	@PostAuthorize("hasRole('ADMIN') or returnObject.body.userId == authentication.principal")
	@GetMapping("/{id}")
	public ResponseEntity<AccountResponse> getAccountById(@PathVariable @P("id") UUID id) {
		return ResponseEntity.status(HttpStatus.OK).body(accountService.getAccountById(id));
	}

	@PreAuthorize("hasRole('ADMIN') or #req.userId == authentication.principal")
	@PostMapping
	public ResponseEntity<AccountResponse> createAccount(@RequestBody @Valid @P("req") CreateAccountRequest req, HttpServletRequest httpRequest) {
		AccountResponse response = accountService.createAccount(req);
		auditService.recordAccountCreated(response.getId(), response.getCurrency().name(), httpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PreAuthorize("hasRole('ADMIN') or @securityService.isAccountOwner(authentication, #id)")
	@GetMapping("/{id}/transactions")
	public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(@PathVariable @P("id") UUID id , Pageable pageable){
		return ResponseEntity.status(HttpStatus.OK).body(transactionService.getTransactionsByAccountId(id , pageable)) ;
	}
}
