package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.helpers.MoneyUtil;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.Customer;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AccountService {
	private final AccountRepository accountRepository ;
	private final CustomerRepository customerRepository ;

	public AccountService(AccountRepository accountRepository , CustomerRepository customerRepository ){
		this.accountRepository = accountRepository ;
		this.customerRepository = customerRepository ;
	}


	public AccountResponse createAccount(CreateAccountRequest req) {
		log.info("Creating account for customer: {}", req.getCustomerId());

		Customer customer = customerRepository.findById(req.getCustomerId())
		                                      .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + req.getCustomerId()));
		Account account = new Account(0L ,req.getCurrency() , customer );
		accountRepository.save(account) ;

		log.info("Successfully created account {} for customer {}", account.getId(), customer.getId());
		return toAccountResponse(account);
	}

	public AccountResponse getAccountById(UUID id){
		log.info("Fetching account details for id: {}", id);

		Account account = accountRepository.findById(id)
		                                   .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
		return toAccountResponse(account);
	}


	public List<AccountResponse> getAccountsByCustomerId(UUID id){
		log.info("Fetching all accounts for customer id: {}", id);
		if(!customerRepository.existsById(id)){
			throw new ResourceNotFoundException("Customer not found with id: " + id) ;
		}
		List<Account> accountsList = accountRepository.findAllByCustomerId(id) ;
		return accountsList.stream()
		                   .map(this::toAccountResponse)
		                   .toList() ;

	}

	// Helper Method
	private AccountResponse toAccountResponse(Account account) {
		return AccountResponse.builder()
		                      .id(account.getId())
		                      .balance(MoneyUtil.formatCents(account.getBalance()))
		                      .currency(account.getCurrency())
		                      .customerId(account.getCustomer().getId())
		                      .build();
	}

}
