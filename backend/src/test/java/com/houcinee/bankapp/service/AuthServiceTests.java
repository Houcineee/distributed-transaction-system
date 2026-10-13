package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.UserResponse;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AuthService authService;

    @Test
    public void givenValidRegistrationRequest_whenRegister_thenHashesPasswordAndCreatesUser() {
        // Arrange
        String email = "test@test.com";
        String name = "test";
        String rawPassword = "securePassword123";
        String hashedPassword = "hashedPasswordXYZ";

        RegisterRequest req = RegisterRequest.builder()
                .email(email)
                .name(name)
                .password(rawPassword)
                .currency(com.houcinee.bankapp.enums.Currency.MAD)
                .cardType(com.houcinee.bankapp.enums.CardType.DEBIT)
                .build();

        UserResponse mockResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .build();

        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userService.createUser(name, email, hashedPassword, Role.CUSTOMER)).thenReturn(mockResponse);

        // Act
        UserResponse response = authService.register(req);

        // Assert
        verify(passwordEncoder).encode(rawPassword);
        verify(userService).createUser(name, email, hashedPassword, Role.CUSTOMER);
        verify(accountService).createAccount(any());
        
        assertEquals(mockResponse.getId(), response.getId());
        assertEquals(mockResponse.getName(), response.getName());
        assertEquals(mockResponse.getEmail(), response.getEmail());
    }
}
