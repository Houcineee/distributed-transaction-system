package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.enums.CardType;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.helpers.MoneyUtil;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class AccountService {
	private final AccountRepository accountRepository ;
	private final UserRepository userRepository;
	private final CardService cardService;

	public AccountService(AccountRepository accountRepository, UserRepository userRepository, CardService cardService){
		this.accountRepository = accountRepository ;
		this.userRepository = userRepository;
		this.cardService = cardService;
	}


	@Transactional
	public AccountResponse createAccount(CreateAccountRequest req) {
		log.info("Creating account for user: {}", req.getUserId());

		AppUser appUser = userRepository.findById(req.getUserId())
		                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + req.getUserId()));
		Account account = new Account(50000L, req.getCurrency(), appUser);
		account = accountRepository.save(account);

		// Automatically issue the requested card type for the new account
		CardType typeToIssue = req.getCardType() != null ? req.getCardType() : CardType.DEBIT;
		cardService.issueCard(account.getId(), typeToIssue);

		log.info("Successfully created account {} for appUser {}", account.getId(), appUser.getId());
		return toAccountResponse(account);
	}

	public AccountResponse getAccountById(UUID id){
		log.info("Fetching account details for id: {}", id);

		Account account = accountRepository.findById(id)
		                                   .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
		return toAccountResponse(account);
	}


	public List<AccountResponse> getAccountsByUserId(UUID id){
		log.info("Fetching all accounts for user id: {}", id);
		if(!userRepository.existsById(id)){
			throw new ResourceNotFoundException("User not found with id: " + id) ;
		}
		List<Account> accountsList = accountRepository.findAllByAppUserId(id) ;
		return accountsList.stream()
		                   .map(this::toAccountResponse)
		                   .toList() ;
	}

	public Page<AccountResponse> getAllAccounts(Pageable pageable) {
		log.info("Fetching all accounts with pagination");
		return accountRepository.findAll(pageable).map(this::toAccountResponse);
	}

	// Helper Method
	private AccountResponse toAccountResponse(Account account) {
		return AccountResponse.builder()
		                      .id(account.getId())
		                      .balance(MoneyUtil.formatCents(account.getBalance()))
		                      .currency(account.getCurrency())
		                      .userId(account.getAppUser().getId())
		                      .build();
	}

}
