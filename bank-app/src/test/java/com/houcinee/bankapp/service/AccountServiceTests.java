package com.houcinee.bankapp.service;


import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.model.Customer;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTests {
	@Mock
	private AccountRepository accountRepository ;
	@Mock
	private CustomerRepository customerRepository ;
	@InjectMocks
	private AccountService accountService  ;


	@Test
	public void givenValidData_whenCreateAccount_thenReturnAccountResponse() {
		Customer customer = new Customer("test" , "test@test.com", "password", com.houcinee.bankapp.enums.Role.USER) ;
		ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
		CreateAccountRequest accountRequest =  CreateAccountRequest.builder()
		                                                           .customerId(customer.getId()).currency(Currency.MAD)
		                                                           .build();

		when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer)) ;

		AccountResponse accountResponse = accountService.createAccount(accountRequest) ;


		assertEquals("0.00" , accountResponse.getBalance()) ;
		assertEquals(customer.getId() , accountResponse.getCustomerId() ) ;
	}

	@Test
	public void givenInvalidCustomerId_whenCreateAccount_thenThrowsResourceNotFoundException() {
		CreateAccountRequest accountRequest = CreateAccountRequest.builder()
				.customerId(UUID.randomUUID()).currency(Currency.MAD)
				.build();

		when(customerRepository.findById(accountRequest.getCustomerId())).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> {
			accountService.createAccount(accountRequest);
		});
	}

	@Test
	public void givenValidId_whenGetAccountById_thenReturnAccountResponse() {
		Customer customer = new Customer("test", "test@test.com", "password", com.houcinee.bankapp.enums.Role.USER);
		ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
		
		Account account = new Account(15000L, Currency.MAD, customer);
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());

		when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

		AccountResponse accountResponse = accountService.getAccountById(account.getId());

		assertEquals("150.00", accountResponse.getBalance());
		assertEquals(Currency.MAD, accountResponse.getCurrency());
	}

	@Test
	public void givenInvalidId_whenGetAccountById_thenThrowsResourceNotFoundException() {
		UUID id = UUID.randomUUID();
		when(accountRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> {
			accountService.getAccountById(id);
		});
	}

	@Test
	public void givenValidCustomerId_whenGetAccountsByCustomerId_thenReturnList() {
		UUID customerId = UUID.randomUUID();
		Customer customer = new Customer("test", "test@test.com", "password", com.houcinee.bankapp.enums.Role.USER);
		ReflectionTestUtils.setField(customer, "id", customerId);
		
		Account account = new Account(1000L, Currency.MAD, customer);
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());

		when(customerRepository.existsById(customerId)).thenReturn(true);
		when(accountRepository.findAllByCustomerId(customerId)).thenReturn(List.of(account));

		var responses = accountService.getAccountsByCustomerId(customerId);

		assertEquals(1, responses.size());
		assertEquals("10.00", responses.getFirst().getBalance());
	}

	@Test
	public void givenInvalidCustomerId_whenGetAccountsByCustomerId_thenThrowsResourceNotFoundException() {
		UUID id = UUID.randomUUID();
		when(customerRepository.existsById(id)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> {
			accountService.getAccountsByCustomerId(id);
		});
	}
}