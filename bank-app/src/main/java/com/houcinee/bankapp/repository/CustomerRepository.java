package com.houcinee.bankapp.repository;

import com.houcinee.bankapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

	Boolean existsCustomerByEmail(String email);
	Optional<Customer> findByEmail(String email) ;

}
