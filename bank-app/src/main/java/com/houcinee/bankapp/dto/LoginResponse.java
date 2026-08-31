package com.houcinee.bankapp.dto;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoginResponse {
	private String token ;
}
