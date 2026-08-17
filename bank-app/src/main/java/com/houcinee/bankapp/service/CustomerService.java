package com.houcinee.bankapp.service;

import com.houcinee.bankapp.dto.CreateCustomerRequest;
import com.houcinee.bankapp.dto.CustomerResponse;
import com.houcinee.bankapp.exception.DuplicateResourceException;
import com.houcinee.bankapp.exception.ResourceNotFoundException;
import com.houcinee.bankapp.model.Customer;
import com.houcinee.bankapp.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class CustomerService {
	private final CustomerRepository customerRepository;

	public CustomerService(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	public CustomerResponse createCustomer(CreateCustomerRequest req) {
		log.info("Attempting to create customer with email: {}", req.getEmail());

		Boolean exist = customerRepository.existsCustomerByEmail(req.getEmail());

		if (exist) {
			throw new DuplicateResourceException("Customer with email " + req.getEmail() + " already exists");
		}

		Customer newCustomer = new Customer(req.getName(), req.getEmail());

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