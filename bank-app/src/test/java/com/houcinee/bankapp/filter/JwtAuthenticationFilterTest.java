package com.houcinee.bankapp.filter;

import com.houcinee.bankapp.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void givenNoAuthorizationHeader_whenFilter_thenPassesThrough() throws ServletException, IOException {
		// No Authorization header set

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext()
		                                .getAuthentication());
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtService);
	}

	@Test
	void givenNonBearerAuthorizationHeader_whenFilter_thenPassesThrough() throws ServletException, IOException {
		request.addHeader("Authorization", "Basic some-credentials");

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext()
		                                .getAuthentication());
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtService);
	}

	@Test
	void givenValidToken_whenFilter_thenAuthenticatesUser() throws ServletException, IOException {
		String token = "valid.jwt.token";
		request.addHeader("Authorization", "Bearer " + token);

		Claims claims = new DefaultClaims(Map.of("sub", "alice@bank.com", "userId", "550e8400-e29b-41d4-a716-446655440000", "roles", List.of("ROLE_CUSTOMER")));

		when(jwtService.extractAllClaims(token)).thenReturn(claims);

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		var authentication = SecurityContextHolder.getContext()
		                                          .getAuthentication();
		assertNotNull(authentication);
		assertTrue(authentication.isAuthenticated());
		assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), authentication.getPrincipal());
		assertEquals(1, authentication.getAuthorities()
		                              .size());
		assertEquals("ROLE_CUSTOMER", authentication.getAuthorities()
		                                        .iterator()
		                                        .next()
		                                        .getAuthority());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void givenTokenWithMultipleRoles_whenFilter_thenAssignsAllRoles() throws ServletException, IOException {
		String token = "valid.jwt.token";
		request.addHeader("Authorization", "Bearer " + token);

		Claims claims = new DefaultClaims(Map.of("sub", "admin@bank.com", "userId", "550e8400-e29b-41d4-a716-446655440000", "roles", List.of("ROLE_CUSTOMER", "ROLE_ADMIN")));

		when(jwtService.extractAllClaims(token)).thenReturn(claims);

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		var authentication = SecurityContextHolder.getContext()
		                                          .getAuthentication();
		assertNotNull(authentication);
		assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), authentication.getPrincipal());
		assertEquals(2, authentication.getAuthorities()
		                              .size());
		var authorityNames = authentication.getAuthorities()
		                                   .stream()
		                                   .map(Object::toString)
		                                   .toList();
		assertTrue(authorityNames.contains("ROLE_CUSTOMER"));
		assertTrue(authorityNames.contains("ROLE_ADMIN"));
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void givenExpiredToken_whenFilter_thenDoesNotAuthenticate() throws ServletException, IOException {
		String token = "expired.jwt.token";
		request.addHeader("Authorization", "Bearer " + token);

		when(jwtService.extractAllClaims(token)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext()
		                                .getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void givenTamperedToken_whenFilter_thenDoesNotAuthenticate() throws ServletException, IOException {
		String token = "tampered.jwt.token";
		request.addHeader("Authorization", "Bearer " + token);

		when(jwtService.extractAllClaims(token)).thenThrow(new SignatureException("Invalid signature"));

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext()
		                                .getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void givenTokenWithoutRolesClaim_whenFilter_thenAuthenticatesWithoutAuthorities() throws ServletException, IOException {
		String token = "no.roles.token";
		request.addHeader("Authorization", "Bearer " + token);

		// Token has a subject but no roles claim
		Claims claims = new DefaultClaims(Map.of("sub", "legacy@bank.com", "userId", "550e8400-e29b-41d4-a716-446655440000"));

		when(jwtService.extractAllClaims(token)).thenReturn(claims);

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		var authentication = SecurityContextHolder.getContext()
		                                          .getAuthentication();
		assertNotNull(authentication);
		assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), authentication.getPrincipal());
		assertTrue(authentication.getAuthorities()
		                         .isEmpty());
		verify(filterChain).doFilter(request, response);
	}
}
