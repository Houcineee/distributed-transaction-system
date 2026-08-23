package com.houcinee.bankapp.service;


import com.houcinee.bankapp.dto.CreateCustomerRequest;
import com.houcinee.bankapp.dto.CustomerResponse;
import com.houcinee.bankapp.exception.DuplicateResourceException;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Customer;
import com.houcinee.bankapp.repository.CustomerRepository;
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
public class CustomerServiceTests {
	@Mock
	private CustomerRepository customerRepository;

	@InjectMocks
	private CustomerService customerService;

	@Test
	public void givenValidData_whenCreateCustomer_thenReturnsCustomerResponse(){
		String email = "test@test.com" ;
		String name = "test" ;
		CreateCustomerRequest req = CreateCustomerRequest.builder()
		                                                 .email(email)
		                                                 .name(name).build();
		Customer customerMock = new Customer(name , email) ;
		ReflectionTestUtils.setField(customerMock, "id", UUID.randomUUID());
		when(customerRepository.existsCustomerByEmail(email)).thenReturn(false) ;
		when(customerRepository.save(any(Customer.class))).thenReturn(customerMock) ;

		CustomerResponse res = customerService.createCustomer(req) ;


		verify(customerRepository).existsCustomerByEmail(email);
		verify(customerRepository).save(any(Customer.class));
		assertEquals(customerMock.getId() , res.getId()) ;
		assertEquals(customerMock.getName() , res.getName());
		assertEquals(customerMock.getEmail() , res.getEmail());

	}

	@Test
	public void givenExistingEmail_whenCreateCustomer_thenThrowsDuplicateResourceException(){
		String email = "test@test.com" ;
		String name = "test" ;
		CreateCustomerRequest req = CreateCustomerRequest.builder()
		                                                 .email(email)
		                                                 .name(name).build();
		when(customerRepository.existsCustomerByEmail(email)).thenReturn(true);

		Exception exception = assertThrows(DuplicateResourceException.class , ()-> customerService.createCustomer(req)) ;

		verify(customerRepository).existsCustomerByEmail(email);
		verify(customerRepository, never()).save(any(Customer.class));
		assertEquals("Customer with email " + req.getEmail() + " already exists" , exception.getMessage());

	}

	@Test
	public void givenExistingId_whenGetCustomer_thenReturnsCustomerResponse(){
		String email = "test@test.com" ;
		String name = "test" ;

		Customer customerMock = new Customer(name , email) ;
		ReflectionTestUtils.setField(customerMock, "id", UUID.randomUUID());
		when(customerRepository.findById(customerMock.getId())).thenReturn(Optional.of(customerMock)) ;

		CustomerResponse customer = customerService.getCustomerById(customerMock.getId()) ;

		assertEquals(customerMock.getEmail() , customer.getEmail());
		assertEquals(customerMock.getName() , customer.getName());
		assertEquals(customerMock.getId() , customer.getId());
	}

	@Test
	public void givenNonExistingId_whenGetCustomer_thenThrowsResourceNotFoundException(){
		UUID randomId = UUID.randomUUID() ;

		when(customerRepository.findById(randomId)).thenReturn(Optional.empty()) ;

		Exception exception = assertThrows(ResourceNotFoundException.class , ()-> customerService.getCustomerById(randomId)) ;

		assertEquals("Customer not found with id: " + randomId , exception.getMessage());

	}
}
