package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.AuditLogResponse;
import com.houcinee.bankapp.enums.AuditAction;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
public class AdminController {
	private final AccountService accountService;
	private final AuditService auditService;

	public AdminController(AccountService accountService, AuditService auditService) {
		this.accountService = accountService;
		this.auditService = auditService;
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/accounts")
	public ResponseEntity<Page<AccountResponse>> getAllAccounts(Pageable pageable) {
		log.info("Admin request received: Fetching all accounts with pagination");
		return ResponseEntity.status(HttpStatus.OK).body(accountService.getAllAccounts(pageable));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/audit-logs")
	public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
			@RequestParam(required = false) UUID actorId,
			@RequestParam(required = false) AuditAction action,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
			Pageable pageable) {
		log.info("Admin request received: Fetching audit logs with filters [actorId={}, action={}, startDate={}, endDate={}]", 
				actorId, action, startDate, endDate);
		return ResponseEntity.ok(auditService.getAuditLogs(actorId, action, startDate, endDate, pageable));
	}
}
