package com.houcinee.bankapp.service;


import com.houcinee.bankapp.dto.UserResponse;
import com.houcinee.bankapp.enums.Role;
import com.houcinee.bankapp.exception.DuplicateResourceException;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.AppUser;
import com.houcinee.bankapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTests {
	@Mock
	private UserRepository userRepository;
	@InjectMocks
	private UserService userService;

	@Test
	public void givenValidData_whenCreateUser_thenReturnsUserResponse(){
		String email = "test@test.com" ;
		String name = "test" ;
		String hashedPassword = "hashedPassword";
		AppUser appUserMock = new AppUser(name , email, hashedPassword, Role.CUSTOMER) ;
		ReflectionTestUtils.setField(appUserMock, "id", UUID.randomUUID());
		when(userRepository.existsByEmail(email)).thenReturn(false) ;
		when(userRepository.save(any(AppUser.class))).thenReturn(appUserMock) ;

		UserResponse res = userService.createUser(name, email, hashedPassword, Role.CUSTOMER) ;

		verify(userRepository).existsByEmail(email);
		verify(userRepository).save(any(AppUser.class));
		assertEquals(appUserMock.getId() , res.getId()) ;
		assertEquals(appUserMock.getName() , res.getName());
		assertEquals(appUserMock.getEmail() , res.getEmail());
	}

	@Test
	public void givenExistingEmail_whenCreateUser_thenThrowsDuplicateResourceException(){
		String email = "test@test.com" ;
		String name = "test" ;
		String hashedPassword = "hashedPassword";
		when(userRepository.existsByEmail(email)).thenReturn(true);

		Exception exception = assertThrows(DuplicateResourceException.class , ()-> userService.createUser(name, email, hashedPassword, Role.CUSTOMER)) ;

		verify(userRepository).existsByEmail(email);
		verify(userRepository, never()).save(any(AppUser.class));
		assertEquals("User with email " + email + " already exists" , exception.getMessage());
	}

	@Test
	public void givenExistingId_whenGetUser_thenReturnsUserResponse(){
		String email = "test@test.com" ;
		String name = "test" ;

		AppUser appUserMock = new AppUser(name , email, "password", Role.CUSTOMER) ;
		ReflectionTestUtils.setField(appUserMock, "id", UUID.randomUUID());
		when(userRepository.findById(appUserMock.getId())).thenReturn(Optional.of(appUserMock)) ;

		UserResponse user = userService.getUserById(appUserMock.getId()) ;

		assertEquals(appUserMock.getEmail() , user.getEmail());
		assertEquals(appUserMock.getName() , user.getName());
		assertEquals(appUserMock.getId() , user.getId());
	}

	@Test
	public void givenNonExistingId_whenGetUser_thenThrowsResourceNotFoundException(){
		UUID randomId = UUID.randomUUID() ;

		when(userRepository.findById(randomId)).thenReturn(Optional.empty()) ;

		Exception exception = assertThrows(ResourceNotFoundException.class , ()-> userService.getUserById(randomId)) ;

		assertEquals("User not found with id: " + randomId , exception.getMessage());

	}
}
