package com.houcinee.bankapp.repository;

import com.houcinee.bankapp.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Account> findAccountById(UUID id);

	List<Account> findAllByAppUserId(UUID id);


}
