package com.houcinee.bankapp.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private final String testSecretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Use ReflectionTestUtils to inject the @Value properties for unit testing
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        when(userDetails.getUsername()).thenReturn("test@bank.com");

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("test@bank.com", extractedUsername);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldThrowExceptionWhenTokenIsTampered() {
        when(userDetails.getUsername()).thenReturn("test@bank.com");
        String validToken = jwtService.generateToken(userDetails);

        // Hacker intercepts and changes the last character
        String tamperedToken = validToken.substring(0, validToken.length() - 1) + "A";

        // JJWT throws a SignatureException when the hash doesn't match
        assertThrows(SignatureException.class, () -> {
            jwtService.extractUsername(tamperedToken);
        });
    }

    @Test
    void shouldThrowExceptionWhenTokenIsExpired() {
        when(userDetails.getUsername()).thenReturn("test@bank.com");

        // Set expiration to 1 second in the past (-1000 ms)
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);

        String expiredToken = jwtService.generateToken(userDetails);

        // JJWT actively throws an ExpiredJwtException when parsing an expired token
        assertThrows(ExpiredJwtException.class, () -> {
            jwtService.extractUsername(expiredToken);
        });
    }
}
