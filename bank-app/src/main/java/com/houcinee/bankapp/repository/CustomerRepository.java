package com.houcinee.bankapp.repository;

import com.houcinee.bankapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public abstract interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
