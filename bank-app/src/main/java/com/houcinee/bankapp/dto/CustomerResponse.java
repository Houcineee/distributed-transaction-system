package com.houcinee.bankapp.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class CustomerResponse {
	private UUID id;
	private String name ;
	private String email;
	private Instant createdAt;
}
