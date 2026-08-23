package com.houcinee.bankapp.controller;


import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.CreateCustomerRequest;
import com.houcinee.bankapp.dto.CustomerResponse;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class CustomerController {
	private final CustomerService customerService;
	private final AccountService accountService ;

	public CustomerController(CustomerService customerService , AccountService accountService) {
		this.customerService = customerService;
		this.accountService = accountService ;
	}

	@GetMapping("/api/v1/customers/{id}")
	ResponseEntity<CustomerResponse> getCustomerById(@PathVariable UUID id) {
		return ResponseEntity.status(200).body(customerService.getCustomerById(id));
	}

	@PostMapping("/api/v1/customers")
	ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest req) {
		return ResponseEntity.status(201).body(customerService.createCustomer(req));
	}

	@GetMapping("/api/v1/customers/{id}/accounts")
	ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(@PathVariable UUID id){
		return ResponseEntity.status(HttpStatus.OK)
		                     .body(accountService.getAccountsByCustomerId(id)) ;
	}


}
