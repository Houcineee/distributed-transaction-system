package com.houcinee.bankapp.security;

import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.model.Card;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private SecurityService securityService;

    @Test
    void givenNullAuthentication_whenIsAccountOwner_thenReturnsFalse() {
        UUID accountId = UUID.randomUUID();
        
        boolean result = securityService.isAccountOwner(null, accountId);
        
        assertFalse(result);
    }

    @Test
    void givenUnauthenticatedUser_whenIsAccountOwner_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        UUID accountId = UUID.randomUUID();
        
        boolean result = securityService.isAccountOwner(auth, accountId);
        
        assertFalse(result);
    }

    @Test
    void givenAccountDoesNotExist_whenIsAccountOwner_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        UUID loggedInUserId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(loggedInUserId);

        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        boolean result = securityService.isAccountOwner(auth, accountId);
        
        assertFalse(result);
    }

    @Test
    void givenAccountExistsButDifferentUser_whenIsAccountOwner_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        UUID loggedInUserId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(loggedInUserId);

        UUID accountId = UUID.randomUUID();
        
        AppUser differentUser = new AppUser();
        ReflectionTestUtils.setField(differentUser, "id", UUID.randomUUID());
        
        Account account = new Account();
        ReflectionTestUtils.setField(account, "appUser", differentUser);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        boolean result = securityService.isAccountOwner(auth, accountId);
        
        assertFalse(result);
    }

    @Test
    void givenAccountExistsAndSameUser_whenIsAccountOwner_thenReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        UUID loggedInUserId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(loggedInUserId);

        UUID accountId = UUID.randomUUID();
        
        AppUser sameUser = new AppUser();
        ReflectionTestUtils.setField(sameUser, "id", loggedInUserId);
        
        Account account = new Account();
        ReflectionTestUtils.setField(account, "appUser", sameUser);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        boolean result = securityService.isAccountOwner(auth, accountId);
        
        assertTrue(result);
    }

    @Test
    void givenNullAuthentication_whenIsCardOwner_thenReturnsFalse() {
        UUID cardId = UUID.randomUUID();

        boolean result = securityService.isCardOwner(null, cardId);

        assertFalse(result);
    }

    @Test
    void givenUnauthenticatedUser_whenIsCardOwner_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        boolean result = securityService.isCardOwner(auth, UUID.randomUUID());

        assertFalse(result);
    }

    @Test
    void givenCardDoesNotExist_whenIsCardOwner_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(UUID.randomUUID());

        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        boolean result = securityService.isCardOwner(auth, cardId);

        assertFalse(result);
    }

    @Test
    void givenCardExistsButDifferentUser_whenIsCardOwner_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        UUID loggedInUserId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(loggedInUserId);

        AppUser differentUser = new AppUser();
        ReflectionTestUtils.setField(differentUser, "id", UUID.randomUUID());

        Account account = new Account();
        ReflectionTestUtils.setField(account, "appUser", differentUser);

        Card card = new Card();
        ReflectionTestUtils.setField(card, "account", account);

        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        boolean result = securityService.isCardOwner(auth, cardId);

        assertFalse(result);
    }

    @Test
    void givenCardExistsAndSameUser_whenIsCardOwner_thenReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        UUID loggedInUserId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(loggedInUserId);

        AppUser sameUser = new AppUser();
        ReflectionTestUtils.setField(sameUser, "id", loggedInUserId);

        Account account = new Account();
        ReflectionTestUtils.setField(account, "appUser", sameUser);

        Card card = new Card();
        ReflectionTestUtils.setField(card, "account", account);

        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        boolean result = securityService.isCardOwner(auth, cardId);

        assertTrue(result);
    }
}
