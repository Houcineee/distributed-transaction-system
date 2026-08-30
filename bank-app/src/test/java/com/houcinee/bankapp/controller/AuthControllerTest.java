package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.dto.CustomerResponse;
import com.houcinee.bankapp.service.AuthService;
import com.houcinee.bankapp.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends BaseWebMvcTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void givenValidRequest_whenCreateCustomer_thenReturns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Houcine")
                .email("houcine@test.com")
                .password("secure123")
                .build();

        CustomerResponse response = CustomerResponse.builder()
                .id(UUID.randomUUID())
                .name("Houcine")
                .email("houcine@test.com")
                .createdAt(Instant.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Houcine"))
                .andExpect(jsonPath("$.email").value("houcine@test.com"));
    }

    @Test
    void givenInvalidRequest_whenCreateCustomer_thenReturns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("")
                .email("not-an-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
