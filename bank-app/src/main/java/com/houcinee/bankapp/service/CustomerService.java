package com.houcinee.bankapp.service;


import com.houcinee.bankapp.dto.CustomerResponse;
import com.houcinee.bankapp.exception.DuplicateResourceException;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Customer;
import com.houcinee.bankapp.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import com.houcinee.bankapp.enums.Role;

@Service
@Slf4j
public class CustomerService {
	private final CustomerRepository customerRepository;

	public CustomerService(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	public CustomerResponse createCustomer(String name, String email, String hashedPassword, Role role) {
		log.info("Attempting to create customer with email: {}", email);

		Boolean exist = customerRepository.existsCustomerByEmail(email);

		if (exist) {
			throw new DuplicateResourceException("Customer with email " + email + " already exists");
		}

		Customer newCustomer = new Customer(name, email, hashedPassword, role);

		Customer savedCustomer = customerRepository.save(newCustomer);
		log.info("Successfully created customer with id: {}", savedCustomer.getId());

		return toCustomerResponse(savedCustomer);

	}

	public CustomerResponse getCustomerById(UUID id) {
		log.info("Fetching customer profile for id: {}", id);

		Customer customer = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

		return toCustomerResponse(customer);
	}

	//Helper Method
	private CustomerResponse toCustomerResponse(Customer customer) {
		return CustomerResponse.builder().id(customer.getId()).name(customer.getName()).email(customer.getEmail()).createdAt(customer.getCreatedAt()).build();
	}
}