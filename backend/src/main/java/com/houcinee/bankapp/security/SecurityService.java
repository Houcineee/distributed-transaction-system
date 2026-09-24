package com.houcinee.bankapp.security;

import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;

    public boolean isAccountOwner(Authentication authentication, UUID accountId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        UUID currentUserId = (UUID) authentication.getPrincipal();

        return accountRepository.findById(accountId)
                .map(account -> account.getAppUser().getId().equals(currentUserId))
                .orElse(false);
    }

    public boolean isCardOwner(Authentication authentication, UUID cardId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        UUID currentUserId = (UUID) authentication.getPrincipal();

        return cardRepository.findById(cardId)
                .map(card -> card.getAccount().getAppUser().getId().equals(currentUserId))
                .orElse(false);
    }
}
