package com.houcinee.bankapp.dto;


import com.houcinee.bankapp.enums.CardType;
import com.houcinee.bankapp.enums.Currency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
	private Currency currency;
	
	@NotNull
	private CardType cardType;
}
