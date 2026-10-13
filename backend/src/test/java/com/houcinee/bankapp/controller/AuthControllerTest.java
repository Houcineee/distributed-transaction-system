package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.LoginRequest;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.dto.UserResponse;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends BaseWebMvcTest {

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@Test
	void givenValidRegistrationRequest_whenRegister_thenReturns201() throws Exception {
		RegisterRequest request = RegisterRequest.builder()
		                                         .name("Houcine")
		                                         .email("houcine@test.com")
		                                         .password("secure123")
		                                         .currency(com.houcinee.bankapp.enums.Currency.MAD)
		                                         .cardType(com.houcinee.bankapp.enums.CardType.DEBIT)
		                                         .build();

		UserResponse response = UserResponse.builder()
		                                            .id(UUID.randomUUID())
		                                            .name("Houcine")
		                                            .email("houcine@test.com")
		                                            .createdAt(Instant.now())
		                                            .build();

		when(authService.register(any(RegisterRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
		                                             .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isCreated())
		       .andExpect(jsonPath("$.name").value("Houcine"))
		       .andExpect(jsonPath("$.email").value("houcine@test.com"));

		verify(auditService).recordUserRegistered(eq(response.getId()), eq("houcine@test.com"), any());
	}

	@Test
	void givenInvalidRegistrationRequest_whenRegister_thenReturns400() throws Exception {
		RegisterRequest request = RegisterRequest.builder()
		                                         .name("")
		                                         .email("not-an-email")
		                                         .password("short")
		                                         .build();

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
		                                             .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isBadRequest());
	}

	@Test
	void givenValidLoginRequest_whenLogin_thenReturns200AndTokenCookie() throws Exception {
		LoginRequest request = LoginRequest.builder()
		                                   .email("houcine@test.com")
		                                   .password("secure123")
		                                   .build();

		Authentication mockAuth = Mockito.mock(Authentication.class);
		AppUser mockUser = new AppUser();
		when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
		when(mockAuth.getPrincipal()).thenReturn(mockUser);
		when(jwtService.generateToken(mockUser)).thenReturn("fake-jwt-token");

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
		                                          .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isOk())
		       .andExpect(cookie().value("jwt", "fake-jwt-token"));

		verify(auditService).recordLoginSuccess(any());
	}

	@Test
	void givenLogoutRequest_whenLogout_thenReturns200AndClearsCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
		       .andExpect(status().isOk())
		       .andExpect(cookie().maxAge("jwt", 0));
	}

	@Test
	void givenInvalidLoginRequest_whenLogin_thenpageableReturns400() throws Exception {
		LoginRequest request = LoginRequest.builder()
		                                   .email("not-an-email")
		                                   .password("")
		                                   .build();

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
		                                          .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isBadRequest());
	}

	@Test
	void givenWrongCredentials_whenLogin_thenReturns401() throws Exception {
		LoginRequest request = LoginRequest.builder()
		                                   .email("houcine@test.com")
		                                   .password("wrongpassword")
		                                   .build();

		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
		                                          .content(objectMapper.writeValueAsString(request)))
		       .andExpect(status().isUnauthorized())
		       .andExpect(jsonPath("$.message").value("Invalid email or password"));
	}
}
