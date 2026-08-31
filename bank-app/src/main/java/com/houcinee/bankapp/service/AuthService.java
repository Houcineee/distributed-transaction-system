package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.UserResponse;
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

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest req) {
        log.info("Registering new user with email: {}", req.getEmail());
        String hashedPassword = passwordEncoder.encode(req.getPassword());
        return userService.createUser(req.getName(), req.getEmail(), hashedPassword, Role.CUSTOMER);
    }
}
