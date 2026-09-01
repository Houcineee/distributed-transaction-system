package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.dto.UserResponse;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest extends BaseWebMvcTest {

	@MockitoBean
	private UserService userService;
	@MockitoBean
	private AccountService accountService;

	// --- getUserById Tests ---
	@Test
	void givenSameUser_whenGetUserById_thenReturns200() throws Exception {
		UUID userId = UUID.randomUUID();
		UserResponse res = UserResponse.builder().name("test").email("test@test.com").id(userId).build();

		when(userService.getUserById(userId)).thenReturn(res);

		mockMvc.perform(get("/api/v1/users/" + userId).with(mockAuth(userId, "CUSTOMER"))
		                                                .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.id").value(userId.toString()));
	}

	@Test
	void givenDifferentUser_whenGetUserById_thenReturns403() throws Exception {
		UUID targetUserId = UUID.randomUUID();
		UUID loggedInUserId = UUID.randomUUID(); // Different ID!

		mockMvc.perform(get("/api/v1/users/" + targetUserId).with(mockAuth(loggedInUserId, "CUSTOMER"))
		                                                    .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isForbidden());
	}

	@Test
	void givenInvalidId_whenGetUserById_thenReturns400() throws Exception {
		String randomId = "not-a-valid-id";
		// Admin role bypasses security to hit the validation 400 Bad Request
		mockMvc.perform(get("/api/v1/users/" + randomId).with(mockAuth(UUID.randomUUID(), "ADMIN"))
		                                                .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isBadRequest());
	}

	// --- getAccountsByUserId Tests ---
	@Test
	void givenSameUser_whenGetAccountsByUserId_thenReturns200() throws Exception {
		UUID userId = UUID.randomUUID();
		when(accountService.getAccountsByUserId(userId)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/users/" + userId + "/accounts").with(mockAuth(userId, "CUSTOMER"))
		                                                            .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk());
	}

	@Test
	void givenDifferentUser_whenGetAccountsByUserId_thenReturns403() throws Exception {
		UUID targetUserId = UUID.randomUUID();
		UUID loggedInUserId = UUID.randomUUID(); // Different ID!

		mockMvc.perform(get("/api/v1/users/" + targetUserId + "/accounts").with(mockAuth(loggedInUserId, "CUSTOMER"))
		                                                                  .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isForbidden());
	}
}
