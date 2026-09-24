package com.houcinee.bankapp.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RegisterRequest {
	@NotBlank
	private String name;
	@NotBlank
	@Email
	private String email;
	@NotBlank
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String password;
	
	@NotNull
	private com.houcinee.bankapp.enums.Currency currency;
	
	@NotNull
	private com.houcinee.bankapp.enums.CardType cardType;
	
	private String cardholderName;
}
