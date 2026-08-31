package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.UserResponse;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

	@Test
	void givenValidId_whenGetUser_thenReturns200() throws Exception {
		UUID randomId = UUID.randomUUID();
		String name = "test";
		String email = "test@test.com";
		UserResponse res = UserResponse.builder()
		                                       .name(name)
		                                       .email(email)
		                                       .id(randomId)
		                                       .build();

		when(userService.getUserById(randomId)).thenReturn(res);

		mockMvc.perform(get("/api/v1/users/" + randomId).contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.name").value(name))
		       .andExpect(jsonPath("$.id").value(randomId.toString()));
	}

	@Test
	void givenInvalidId_whenGetUser_thenReturns400() throws Exception {
		String randomId = "not-a-valid-id";

		mockMvc.perform(get("/api/v1/users/" + randomId).contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isBadRequest());
	}
}
