package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.AccountResponse;
import com.houcinee.bankapp.service.AccountService;
import com.houcinee.bankapp.dto.AuditLogResponse;
import com.houcinee.bankapp.enums.AuditAction;
import com.houcinee.bankapp.enums.Currency;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest extends BaseWebMvcTest {

	@MockitoBean
	private AccountService accountService;

	@Test
	void givenAdmin_whenGetAllAccounts_thenReturns200() throws Exception {
		UUID accountId = UUID.randomUUID();
		AccountResponse account = AccountResponse.builder()
				.id(accountId)
				.balance("1500.00")
				.currency(Currency.USD)
				.userId(UUID.randomUUID())
				.build();
		Page<AccountResponse> page = new PageImpl<>(List.of(account), PageRequest.of(0, 20), 1);
		when(accountService.getAllAccounts(any())).thenReturn(page);

		mockMvc.perform(get("/api/v1/admin/accounts").param("page", "0")
		                                             .with(mockAuth(UUID.randomUUID(), "ADMIN"))
		                                             .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.content[0].id").value(accountId.toString()))
		       .andExpect(jsonPath("$.content[0].balance").value("1500.00"))
		       .andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void givenCustomer_whenGetAllAccounts_thenReturns403() throws Exception {
		mockMvc.perform(get("/api/v1/admin/accounts").param("page", "0")
		                                             .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
		                                             .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isForbidden());
	}

	@Test
	void givenAdmin_whenGetAuditLogs_thenReturns200() throws Exception {
		AuditLogResponse logResponse = AuditLogResponse.builder()
				.id(UUID.randomUUID())
				.action(AuditAction.TRANSFER_EXECUTED)
				.ipAddress("127.0.0.1")
				.build();
		Page<AuditLogResponse> page = new PageImpl<>(List.of(logResponse), PageRequest.of(0, 20), 1);
		when(auditService.getAuditLogs(any(), any(), any(), any(), any())).thenReturn(page);

		mockMvc.perform(get("/api/v1/admin/audit-logs")
		               .param("page", "0")
		               .param("action", "TRANSFER_EXECUTED")
		               .with(mockAuth(UUID.randomUUID(), "ADMIN"))
		               .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.content[0].action").value("TRANSFER_EXECUTED"))
		       .andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void givenCustomer_whenGetAuditLogs_thenReturns403() throws Exception {
		mockMvc.perform(get("/api/v1/admin/audit-logs")
		               .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
		               .contentType(MediaType.APPLICATION_JSON))
		       .andExpect(status().isForbidden());
	}
}
