package com.houcinee.bankapp.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateCustomerRequest {
	@NotBlank
	private String name;
	@NotBlank
	@Email
	private String email;
}
