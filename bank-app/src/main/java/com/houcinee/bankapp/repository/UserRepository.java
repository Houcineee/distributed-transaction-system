package com.houcinee.bankapp.repository;

import com.houcinee.bankapp.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {

	Boolean existsByEmail(String email);
	Optional<AppUser> findByEmail(String email) ;

}
