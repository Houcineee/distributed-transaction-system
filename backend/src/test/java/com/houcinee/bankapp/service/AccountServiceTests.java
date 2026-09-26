package com.houcinee.bankapp.service;


import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.enums.CardType;
import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.enums.Role;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTests {
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private CardService cardService;
	@InjectMocks
	private AccountService accountService;


	@Test
	public void givenValidData_whenCreateAccount_thenReturnsAccountResponse() {
		AppUser appUser = new AppUser("test", "test@test.com", "password", Role.CUSTOMER);
		ReflectionTestUtils.setField(appUser, "id", UUID.randomUUID());
		CreateAccountRequest accountRequest = CreateAccountRequest.builder()
		                                                          .userId(appUser.getId())
		                                                          .currency(Currency.MAD)
		                                                          .build();

		when(userRepository.findById(appUser.getId())).thenReturn(Optional.of(appUser));
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
			Account acc = invocation.getArgument(0);
			ReflectionTestUtils.setField(acc, "id", UUID.randomUUID());
			return acc;
		});

		AccountResponse accountResponse = accountService.createAccount(accountRequest);


		assertEquals("0.00", accountResponse.getBalance());
		assertEquals(appUser.getId(), accountResponse.getUserId());

		// Verify that a DEBIT card was automatically issued for the new account
		verify(cardService).issueCard(any(UUID.class), eq(CardType.DEBIT));
	}

	@Test
	public void givenInvalidUserId_whenCreateAccount_thenThrowsResourceNotFoundException() {
		CreateAccountRequest accountRequest = CreateAccountRequest.builder()
		                                                          .userId(UUID.randomUUID())
		                                                          .currency(Currency.MAD)
		                                                          .build();

		when(userRepository.findById(accountRequest.getUserId())).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> {
			accountService.createAccount(accountRequest);
		});
	}

	@Test
	public void givenValidId_whenGetAccountById_thenReturnsAccountResponse() {
		AppUser appUser = new AppUser("test", "test@test.com", "password", Role.CUSTOMER);
		ReflectionTestUtils.setField(appUser, "id", UUID.randomUUID());

		Account account = new Account(15000L, Currency.MAD, appUser);
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
	public void givenValidUserId_whenGetAccountsByUserId_thenReturnsAccountList() {
		UUID userId = UUID.randomUUID();
		AppUser appUser = new AppUser("test", "test@test.com", "password", Role.CUSTOMER);
		ReflectionTestUtils.setField(appUser, "id", userId);

		Account account = new Account(1000L, Currency.MAD, appUser);
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());

		when(userRepository.existsById(userId)).thenReturn(true);
		when(accountRepository.findAllByAppUserId(userId)).thenReturn(List.of(account));

		var responses = accountService.getAccountsByUserId(userId);

		assertEquals(1, responses.size());
		assertEquals("10.00", responses.getFirst()
		                               .getBalance());
	}

	@Test
	public void givenInvalidUserId_whenGetAccountsByUserId_thenThrowsResourceNotFoundException() {
		UUID id = UUID.randomUUID();
		when(userRepository.existsById(id)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> {
			accountService.getAccountsByUserId(id);
		});
	}

	@Test
	public void givenPageable_whenGetAllAccounts_thenReturnsPagedAccountResponses() {
		Pageable pageable = PageRequest.of(0, 10);

		AppUser appUser = new AppUser("test", "test@test.com", "password", Role.ADMIN);
		ReflectionTestUtils.setField(appUser, "id", UUID.randomUUID());

		Account account = new Account(1000L, Currency.MAD, appUser);
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());

		Page<Account> pagedAccounts = new PageImpl<>(List.of(account));

		when(accountRepository.findAll(pageable)).thenReturn(pagedAccounts);

		Page<AccountResponse> responsePage = accountService.getAllAccounts(pageable);

		assertEquals(1, responsePage.getTotalElements());
		assertEquals("10.00", responsePage.getContent()
		                                  .getFirst()
		                                  .getBalance());
	}
}
