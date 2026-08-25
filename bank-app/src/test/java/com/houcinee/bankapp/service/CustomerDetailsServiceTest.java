package com.houcinee.bankapp.service;

import com.houcinee.bankapp.model.Customer;
import com.houcinee.bankapp.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerDetailsServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerDetailsService customerDetailsService;

    @Test
    void loadUserByUsername_Success() {
        // Arrange
        String email = "houcine@bank.com";
        Customer mockCustomer = new Customer("Houcine", email);
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(mockCustomer));

        // Act
        UserDetails userDetails = customerDetailsService.loadUserByUsername(email);

        // Assert
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        verify(customerRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_ThrowsUsernameNotFoundException() {
        // Arrange
        String email = "unknown@bank.com";
        when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, 
                () -> customerDetailsService.loadUserByUsername(email));
        verify(customerRepository, times(1)).findByEmail(email);
    }
}
