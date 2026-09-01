package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransactionController {
	private final TransactionService transactionService ;

	public TransactionController(TransactionService transactionService){
		this.transactionService = transactionService ;
	}

	@PreAuthorize("hasRole('ADMIN') or @securityService.isAccountOwner(authentication, #transactionRequest.srcAccount)")
	@PostMapping
	public ResponseEntity<TransactionResponse> transferMoney(@RequestBody @Valid @P("transactionRequest") TransactionRequest  transactionRequest){
		TransactionResponse transaction = transactionService.transferMoney(transactionRequest.getSrcAccount(),
				transactionRequest.getDestAccount(),
				transactionRequest.getAmount());
		return ResponseEntity.status(HttpStatus.CREATED).body(transaction) ;
	}

}
