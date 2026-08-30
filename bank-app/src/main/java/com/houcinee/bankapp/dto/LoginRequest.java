package com.houcinee.bankapp.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LoginRequest {
	@Email
	private String email ;
	@NotBlank
	@Min(value = 8, message = "Password must be at least 8 characters long")
	private String password ;
}
