package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.CustomerResponse;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;

    public CustomerResponse register(RegisterRequest req) {
        log.info("Registering new customer with email: {}", req.getEmail());
        String hashedPassword = passwordEncoder.encode(req.getPassword());
        return customerService.createCustomer(req.getName(), req.getEmail(), hashedPassword, Role.USER);
    }
}
