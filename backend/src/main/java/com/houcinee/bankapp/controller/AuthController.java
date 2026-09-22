package com.houcinee.bankapp.controller;

import com.houcinee.bankapp.dto.UserResponse;
import com.houcinee.bankapp.dto.LoginRequest;
import com.houcinee.bankapp.dto.LoginResponse;
import com.houcinee.bankapp.dto.RegisterRequest;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.service.AuthService;
import com.houcinee.bankapp.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.houcinee.bankapp.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final AuditService auditService;

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
		UserResponse response = authService.register(request);
		auditService.recordUserRegistered(response.getId(), response.getEmail(), httpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@org.springframework.beans.factory.annotation.Value("${app.security.cookie.secure:false}")
	private boolean secureCookie;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		httpRequest.setAttribute("attemptedEmail", request.getEmail());
		
		Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
		log.info("User {} successfully logged in", request.getEmail());
		String token = jwtService.generateToken((AppUser) authentication.getPrincipal());
		
		auditService.recordLoginSuccess(httpRequest);
		
		ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
		        .httpOnly(true)
		        .secure(secureCookie)
		        .sameSite("Strict")
		        .path("/")
		        .maxAge(7 * 24 * 60 * 60)
		        .build();

		return ResponseEntity.ok()
		        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
		        .body(LoginResponse.builder().build()); // token removed from body
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		ResponseCookie deleteCookie = ResponseCookie.from("jwt", "")
		        .httpOnly(true)
		        .secure(secureCookie)
		        .sameSite("Strict")
		        .path("/")
		        .maxAge(0)
		        .build();
		        
		return ResponseEntity.ok()
		        .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
		        .build();
	}
}
