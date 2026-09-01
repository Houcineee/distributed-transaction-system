package com.houcinee.bankapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.houcinee.bankapp.config.SecurityConfig;
import com.houcinee.bankapp.filter.JwtAuthenticationFilter;
import com.houcinee.bankapp.security.SecurityService;
import com.houcinee.bankapp.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public abstract class BaseWebMvcTest {

	@Autowired
	protected MockMvc mockMvc;

	protected final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	protected JwtService jwtService;

	@MockitoBean
	protected UserDetailsService userDetailsService;

	@MockitoBean(name = "securityService")
	protected SecurityService securityService;

	protected RequestPostProcessor mockAuth(UUID userId, String role) {
		var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
		var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
		return authentication(authToken);
	}
}
