package com.houcinee.bankapp.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
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
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
    }

    @Test
    void shouldGenerateAndExtractClaimsSuccessfully() {
        when(userDetails.getUsername()).thenReturn("test@bank.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        Claims claims = jwtService.extractAllClaims(token);
        
        assertEquals("test@bank.com", claims.getSubject());
        
        List<String> roles = claims.get("roles", List.class);
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    void shouldThrowExceptionWhenTokenIsTampered() {
        when(userDetails.getUsername()).thenReturn("test@bank.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
        String validToken = jwtService.generateToken(userDetails);

        // Hacker intercepts and changes the last character
        String tamperedToken = validToken.substring(0, validToken.length() - 1) + "A";

        assertThrows(SignatureException.class, () -> {
            jwtService.extractAllClaims(tamperedToken);
        });
    }

    @Test
    void shouldThrowExceptionWhenTokenIsExpired() {
        when(userDetails.getUsername()).thenReturn("test@bank.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();

        // Set expiration to 1 second in the past (-1000 ms)
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);

        String expiredToken = jwtService.generateToken(userDetails);

        assertThrows(ExpiredJwtException.class, () -> {
            jwtService.extractAllClaims(expiredToken);
        });
    }
}
