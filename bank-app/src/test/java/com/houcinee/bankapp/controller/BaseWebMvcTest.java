package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

public abstract class BaseWebMvcTest {

	@Autowired
	protected MockMvc mockMvc;

	protected final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	protected JwtService jwtService;

	@MockitoBean
	protected UserDetailsService userDetailsService;
}
