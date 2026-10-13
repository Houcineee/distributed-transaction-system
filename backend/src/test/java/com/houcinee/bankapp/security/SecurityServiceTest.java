package com.houcinee.bankapp.security;

import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
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
        when(accountRepository.existsByIdAndAppUserId(accountId, loggedInUserId)).thenReturn(false);

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
        when(accountRepository.existsByIdAndAppUserId(accountId, loggedInUserId)).thenReturn(false);

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
        when(accountRepository.existsByIdAndAppUserId(accountId, loggedInUserId)).thenReturn(true);

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
        UUID loggedInUserId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(loggedInUserId);

        UUID cardId = UUID.randomUUID();
        when(cardRepository.existsByIdAndAccountAppUserId(cardId, loggedInUserId)).thenReturn(false);

        boolean result = securityService.isCardOwner(auth, cardId);

        assertFalse(result);
    }

    @Test
    void givenCardExistsButDifferentUser_whenIsCardOwner_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        UUID loggedInUserId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(loggedInUserId);

        UUID cardId = UUID.randomUUID();
        when(cardRepository.existsByIdAndAccountAppUserId(cardId, loggedInUserId)).thenReturn(false);

        boolean result = securityService.isCardOwner(auth, cardId);

        assertFalse(result);
    }

    @Test
    void givenCardExistsAndSameUser_whenIsCardOwner_thenReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        UUID loggedInUserId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(loggedInUserId);

        UUID cardId = UUID.randomUUID();
        when(cardRepository.existsByIdAndAccountAppUserId(cardId, loggedInUserId)).thenReturn(true);

        boolean result = securityService.isCardOwner(auth, cardId);

        assertTrue(result);
    }
}
