package com.houcinee.bankapp.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class UserResponse {
	private UUID id;
	private String name ;
	private String email;
	private Instant createdAt;
}
