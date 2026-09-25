package com.houcinee.bankapp.service;


import com.houcinee.bankapp.dto.UserResponse;
import com.houcinee.bankapp.exception.DuplicateResourceException;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import com.houcinee.bankapp.enums.Role;

@Service
@Slf4j
public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserResponse createUser(String name, String email, String hashedPassword, Role role) {
		log.info("Attempting to create user with email: {}", email);

		Boolean exist = userRepository.existsByEmail(email);

		if (exist) {
			throw new DuplicateResourceException("User with email " + email + " already exists");
		}

		AppUser newAppUser = new AppUser(name, email, hashedPassword, role);

		AppUser savedAppUser = userRepository.save(newAppUser);
		log.info("Successfully created user with id: {}", savedAppUser.getId());

		return toUserResponse(savedAppUser);

	}

	public UserResponse getUserById(UUID id) {
		log.info("Fetching user profile for id: {}", id);

		AppUser appUser = userRepository.findById(id)
		                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

		return toUserResponse(appUser);
	}

	//Helper Method
	private UserResponse toUserResponse(AppUser appUser) {
		return UserResponse.builder()
		                       .id(appUser.getId())
		                       .name(appUser.getName())
		                       .email(appUser.getEmail())
		                       .role(appUser.getRole())
		                       .createdAt(appUser.getCreatedAt())
		                       .build();
	}
}
