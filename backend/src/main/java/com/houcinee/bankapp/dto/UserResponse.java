package com.houcinee.bankapp.dto;

import com.houcinee.bankapp.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class UserResponse {
	private UUID id;
	private String name ;
	private String email;
	private Role role;
	private Instant createdAt;
}

