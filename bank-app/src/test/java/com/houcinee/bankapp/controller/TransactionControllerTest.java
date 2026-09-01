package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest extends BaseWebMvcTest {

    @MockitoBean
    private TransactionService transactionService;

    // --- transferMoney Tests ---
    @Test
    void givenAccountOwner_whenTransferMoney_thenReturns200() throws Exception {
        UUID srcAccount = UUID.randomUUID();
        UUID destAccount = UUID.randomUUID();
        
        TransactionRequest request = TransactionRequest.builder()
                .srcAccount(srcAccount)
                .destAccount(destAccount)
                .amount(5000L)
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .id(UUID.randomUUID())
                .amount("50.00")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .sourceAccountId(srcAccount)
                .destinationAccountId(destAccount)
                .build();

        when(transactionService.transferMoney(eq(srcAccount), eq(destAccount), eq(5000L))).thenReturn(response);
        // User OWNS the source account
        when(securityService.isAccountOwner(any(), eq(srcAccount))).thenReturn(true);

        mockMvc.perform(post("/api/v1/transfers")
                .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("50.00"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void givenNotAccountOwner_whenTransferMoney_thenReturns403() throws Exception {
        UUID srcAccount = UUID.randomUUID();
        UUID destAccount = UUID.randomUUID();
        
        TransactionRequest request = TransactionRequest.builder()
                .srcAccount(srcAccount)
                .destAccount(destAccount)
                .amount(5000L)
                .build();

        // User DOES NOT own the source account
        when(securityService.isAccountOwner(any(), eq(srcAccount))).thenReturn(false);

        mockMvc.perform(post("/api/v1/transfers")
                .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNegativeAmount_whenTransferMoney_thenReturns400() throws Exception {
        UUID srcAccount = UUID.randomUUID();
        TransactionRequest request = TransactionRequest.builder()
                .srcAccount(srcAccount)
                .destAccount(UUID.randomUUID())
                .amount(-100L)
                .build();
        
        when(securityService.isAccountOwner(any(), eq(srcAccount))).thenReturn(true);

        mockMvc.perform(post("/api/v1/transfers")
                .with(mockAuth(UUID.randomUUID(), "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
