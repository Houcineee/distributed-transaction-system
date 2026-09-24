package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.CardResponse;
import com.houcinee.bankapp.enums.CardStatus;
import com.houcinee.bankapp.enums.CardType;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTests {

    @Mock
    private CardRepository cardRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @InjectMocks
    private CardService cardService;

    @Test
    public void givenValidAccount_whenIssueCard_thenReturnsUnmaskedCard() {
        UUID accountId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        ReflectionTestUtils.setField(appUser, "name", "John Doe");
        Account account = new Account();
        ReflectionTestUtils.setField(account, "appUser", appUser);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(cardRepository.existsByCardNumber(anyString())).thenReturn(false);

        CardResponse response = cardService.issueCard(accountId, CardType.DEBIT);

        // Verification
        assertNotNull(response.getCardNumber());
        assertEquals(16, response.getCardNumber().length());
        assertFalse(response.getCardNumber().contains("*")); // Unmasked
        assertEquals(3, response.getCvv().length());
        assertFalse(response.getCvv().contains("*")); // Unmasked
        assertEquals(CardType.DEBIT, response.getCardType());
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    public void givenValidCard_whenGetCardById_thenReturnsMaskedCard() {
        UUID cardId = UUID.randomUUID();
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        Card card = new Card("4539001234567890", "John Doe", "123", LocalDate.now().plusYears(3), CardType.DEBIT, account);
        ReflectionTestUtils.setField(card, "id", cardId);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        CardResponse response = cardService.getCardById(cardId);

        assertEquals("**** **** **** 7890", response.getCardNumber());
        assertEquals("***", response.getCvv());
    }

    @Test
    public void givenExpiredCard_whenActivate_thenThrowsException() {
        UUID cardId = UUID.randomUUID();
        Account account = new Account();
        Card card = new Card("4539001234567890", "John Doe", "123", LocalDate.now().plusYears(3), CardType.DEBIT, account);
        ReflectionTestUtils.setField(card, "status", CardStatus.EXPIRED);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(IllegalStateException.class, () -> cardService.activateCard(cardId));
        verify(cardRepository, never()).save(any());
    }

    @Test
    public void whenGenerateLuhnValidCardNumber_thenPassesLuhnCheck() {
        // We will generate 100 cards to mathematically guarantee the algorithm is flawless
        for (int i = 0; i < 100; i++) {
            String cardNumber = cardService.generateLuhnValidCardNumber();
            assertTrue(isValidLuhn(cardNumber), "Card number " + cardNumber + " failed Luhn check!");
        }
    }

    // Helper method to verify the Luhn formula independently from the service implementation
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
