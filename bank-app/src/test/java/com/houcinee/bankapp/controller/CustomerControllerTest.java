package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.dto.CreateCustomerRequest;
import com.houcinee.bankapp.dto.CustomerResponse;
import com.houcinee.bankapp.repository.CustomerRepository;
import com.houcinee.bankapp.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CustomerService customerService;

    @Test
    void givenValidRequest_whenCreateCustomer_thenReturns201() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("Houcine")
                .email("houcine@test.com")
                .build();

        CustomerResponse response = CustomerResponse.builder()
                .id(UUID.randomUUID())
                .name("Houcine")
                .email("houcine@test.com")
                .createdAt(Instant.now())
                .build();

        when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Houcine"))
                .andExpect(jsonPath("$.email").value("houcine@test.com"));
    }

    @Test
    void givenInvalidRequest_whenCreateCustomer_thenReturns400() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("")
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void givenValidId_whenGetCustomer_thenReturns200() throws Exception {
        UUID randomId = UUID.randomUUID() ;
        String name = "test" ;
        String email = "test@test.com" ;
        CustomerResponse res = CustomerResponse.builder().name(name).email(email).id(randomId).build(); ;

        when(customerService.getCustomerById(randomId)).thenReturn(res) ;


        mockMvc.perform(get("/api/v1/customers/" + randomId)
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value(name))
               .andExpect(jsonPath("$.id").value(randomId.toString()));
    }

    @Test
    void givenInvalidId_whenGetCustomer_thenReturns400() throws Exception {
        String randomId = "not-a-valid-id";


        mockMvc.perform(get("/api/v1/customers/" + randomId)
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest()) ;
    }
}
