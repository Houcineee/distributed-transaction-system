package com.houcinee.bankapp.service;

import com.houcinee.bankapp.repository.CustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerDetailsService  implements UserDetailsService {
	private final CustomerRepository customerRepository;

	public CustomerDetailsService(CustomerRepository customerRepository){
		this.customerRepository = customerRepository ;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return customerRepository.findByEmail(email)
		                         .orElseThrow(() -> new UsernameNotFoundException("Customer not found with email: " + email));
	}
}
