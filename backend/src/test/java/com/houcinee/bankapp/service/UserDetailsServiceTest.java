package com.houcinee.bankapp.service;

import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.repository.UserRepository;
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
class UserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @Test
    void givenExistingEmail_whenLoadUserByUsername_thenReturnsUserDetails() {
        // Arrange
        String email = "houcine@bank.com";
        AppUser mockAppUser = new AppUser("Houcine", email, "password", com.houcinee.bankapp.enums.Role.CUSTOMER);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockAppUser));

        // Act
        UserDetails userDetails = appUserDetailsService.loadUserByUsername(email);

        // Assert
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void givenUnknownEmail_whenLoadUserByUsername_thenThrowsUsernameNotFoundException() {
        // Arrange
        String email = "unknown@bank.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, 
                () -> appUserDetailsService.loadUserByUsername(email));
        verify(userRepository, times(1)).findByEmail(email);
    }
}
