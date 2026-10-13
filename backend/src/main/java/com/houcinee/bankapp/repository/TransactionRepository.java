package com.houcinee.bankapp.repository;

import com.houcinee.bankapp.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
	@Query("SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.destAccount.id = :accountId ORDER BY t.createdAt DESC")
	Page<Transaction> findAccountTransactions(@Param("accountId") UUID accountId, Pageable pageable);
}
