package com.houcinee.bankapp.controller;


import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.UserResponse;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
	private final UserService userService;
	private final AccountService accountService ;

	public UserController(UserService userService, AccountService accountService) {
		this.userService = userService;
		this.accountService = accountService ;
	}

	@GetMapping("/{id}")
	ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.getUserById(id));
	}


	@GetMapping("/{id}/accounts")
	ResponseEntity<List<AccountResponse>> getAccountsByUserId(@PathVariable UUID id){
		return ResponseEntity.status(HttpStatus.OK)
		                     .body(accountService.getAccountsByUserId(id)) ;
	}


}
