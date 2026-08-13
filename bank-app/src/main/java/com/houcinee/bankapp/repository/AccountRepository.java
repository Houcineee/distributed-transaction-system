package com.houcinee.bankapp.repository;

import com.houcinee.bankapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account , Long> {
}
