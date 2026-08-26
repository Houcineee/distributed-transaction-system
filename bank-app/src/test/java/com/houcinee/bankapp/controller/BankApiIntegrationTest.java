package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.CreateAccountRequest;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.dto.TransactionRequest;
import com.houcinee.bankapp.enums.Currency;
import com.houcinee.bankapp.model.Account;
import com.houcinee.bankapp.repository.AccountRepository;
import com.houcinee.bankapp.repository.CustomerRepository;
import com.houcinee.bankapp.repository.TransactionRepository;
import com.houcinee.bankapp.service.BaseIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class BankApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc  ;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
    }

    @Test
    void goldenPath_fullEndToEndTransfer() throws Exception {
        // 1. Create Customer A
        String customerAId = createCustomer("Alice", "alice@bank.com");

        // 2. Create Customer B
        String customerBId = createCustomer("Bob", "bob@bank.com");

        // 3. Create Accounts
        String accountAId = createAccount(customerAId);
        String accountBId = createAccount(customerBId);

        // 4. Backdoor: Give Account A $100.00 (10000 cents) so they can transfer
        Account accountA = accountRepository.findById(UUID.fromString(accountAId)).orElseThrow();
        setField(accountA, "balance", 10000L);
        accountRepository.save(accountA);

        // 5. Transfer $50.00
        TransactionRequest transferReq = TransactionRequest.builder()
                .srcAccount(UUID.fromString(accountAId))
                .destAccount(UUID.fromString(accountBId))
                .amount(5000L) // $50.00
                .build();

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("50.00"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 6. Verify Balances via API
        mockMvc.perform(get("/api/v1/accounts/" + accountAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("50.00"));

        mockMvc.perform(get("/api/v1/accounts/" + accountBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("50.00"));
                
        // 7. Verify Transaction History
        mockMvc.perform(get("/api/v1/accounts/" + accountAId + "/transactions?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount").value("50.00"))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    @Test
    void rollbackPath_insufficientFunds() throws Exception {
        // 1. Create Customers & Accounts
        String customerCId = createCustomer("Charlie", "charlie@bank.com");
        String customerDId = createCustomer("Diana", "diana@bank.com");
        String accountCId = createAccount(customerCId); // Balance starts at 0
        String accountDId = createAccount(customerDId); // Balance starts at 0

        // 2. Attempt to transfer $50.00 when balance is 0
        TransactionRequest transferReq = TransactionRequest.builder()
                .srcAccount(UUID.fromString(accountCId))
                .destAccount(UUID.fromString(accountDId))
                .amount(5000L) // $50.00
                .build();

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance in source account"));

        // 3. Verify Balances remained 0 (Rolled back)
        mockMvc.perform(get("/api/v1/accounts/" + accountCId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("0.00"));

        // 4. Verify FAILED transaction was logged
        mockMvc.perform(get("/api/v1/accounts/" + accountCId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    @Test
    void registration_hashesPasswordInDatabase() throws Exception {
        String rawPassword = "superSecretPassword!";
        RegisterRequest req = RegisterRequest.builder()
                .name("Eve")
                .email("eve@hacker.com")
                .password(rawPassword)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String customerIdStr = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        UUID customerId = UUID.fromString(customerIdStr);

        com.houcinee.bankapp.model.Customer savedCustomer = customerRepository.findById(customerId).orElseThrow();
        
        // Assert that the database value is NOT the raw password
        org.junit.jupiter.api.Assertions.assertNotEquals(rawPassword, savedCustomer.getPassword());
        
        // Bcrypt hashes start with $2a$, $2b$, or $2y$
        org.junit.jupiter.api.Assertions.assertTrue(savedCustomer.getPassword().startsWith("$2a$"));
    }

    // --- Helpers to create a customer ---
    private String createCustomer(String name, String email) throws Exception {
        RegisterRequest req = RegisterRequest.builder().name(name).email(email).password("secure123").build();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    // --- Helpers to create an account ---
    private String createAccount(String customerId) throws Exception {
        CreateAccountRequest req = CreateAccountRequest.builder()
                .customerId(UUID.fromString(customerId))
                .currency(Currency.MAD)
                .build();
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
