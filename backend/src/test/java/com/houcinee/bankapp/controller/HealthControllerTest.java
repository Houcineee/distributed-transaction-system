package com.houcinee.bankapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest extends BaseWebMvcTest {

	@Test
	void givenAdmin_whenGetHealth_thenReturns200AndStatusOk() throws Exception {
		mockMvc.perform(get("/api/health")
						.with(mockAuth(UUID.randomUUID(), "ADMIN"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void givenCustomer_whenGetHealth_thenReturns403() throws Exception {
		mockMvc.perform(get("/api/health")
						.with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}
}
