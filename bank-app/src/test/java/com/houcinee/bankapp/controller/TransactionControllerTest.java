package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.dto.TransactionResponse;
import com.houcinee.bankapp.enums.TransactionStatus;
import com.houcinee.bankapp.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void givenValidRequest_whenTransferMoney_thenReturns200() throws Exception {
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

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("50.00"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void givenNegativeAmount_whenTransferMoney_thenReturns400() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .srcAccount(UUID.randomUUID())
                .destAccount(UUID.randomUUID())
                .amount(-100L)
                .build();

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
