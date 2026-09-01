package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest extends BaseWebMvcTest {

	@MockitoBean
	private AccountService accountService;

	@Test
	void givenAdmin_whenGetAllAccounts_thenReturns200() throws Exception {
		Page<AccountResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
		when(accountService.getAllAccounts(any())).thenReturn(page);

		mockMvc.perform(get("/api/v1/admin/accounts").param("page", "0")
		                                             .with(mockAuth(UUID.randomUUID(), "ADMIN"))
		                                             .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk());
	}

	@Test
	void givenCustomer_whenGetAllAccounts_thenReturns403() throws Exception {
		mockMvc.perform(get("/api/v1/admin/accounts").param("page", "0")
		                                             .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
		                                             .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isForbidden());
	}
}
