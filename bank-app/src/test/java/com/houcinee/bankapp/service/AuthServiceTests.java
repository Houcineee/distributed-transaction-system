package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.CustomerResponse;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {

    @Mock
    private CustomerService customerService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    public void givenValidRequest_whenRegister_thenHashesPasswordAndCreatesCustomer() {
        // Arrange
        String email = "test@test.com";
        String name = "test";
        String rawPassword = "securePassword123";
        String hashedPassword = "hashedPasswordXYZ";

        RegisterRequest req = RegisterRequest.builder()
                .email(email)
                .name(name)
                .password(rawPassword)
                .build();

        CustomerResponse mockResponse = CustomerResponse.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .build();

        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(customerService.createCustomer(name, email, hashedPassword, Role.USER)).thenReturn(mockResponse);

        // Act
        CustomerResponse response = authService.register(req);

        // Assert
        verify(passwordEncoder).encode(rawPassword);
        verify(customerService).createCustomer(name, email, hashedPassword, Role.USER);
        
        assertEquals(mockResponse.getId(), response.getId());
        assertEquals(mockResponse.getName(), response.getName());
        assertEquals(mockResponse.getEmail(), response.getEmail());
    }
}
