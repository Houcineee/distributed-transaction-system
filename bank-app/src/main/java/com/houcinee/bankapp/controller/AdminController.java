package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
public class AdminController {
	private final AccountService accountService;

	public AdminController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/accounts")
	public ResponseEntity<Page<AccountResponse>> getAllAccounts(Pageable pageable) {
		log.info("Admin request received: Fetching all accounts with pagination");
		return ResponseEntity.status(HttpStatus.OK).body(accountService.getAllAccounts(pageable));
	}
}
