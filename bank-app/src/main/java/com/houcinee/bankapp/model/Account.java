package com.houcinee.bankapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@NotNull
	@Column(nullable = false)
	private Long balance;

	@NotNull
	@Column(nullable = false)
	private String currency;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	Customer customer;

	@OneToMany(mappedBy = "sourceAccount")
	private List<Transaction> sourceTransactions;

	@OneToMany(mappedBy = "destAccount")
	private List<Transaction> destinationTransactions;

	public void debit(Long amount) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Balance cannot be negative or zero");
		}
		if (amount > this.balance) {
			throw new IllegalArgumentException("Insufficient balance in source account");
		}
		balance -= amount;
	}

	public void credit(Long amount) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Balance cannot be negative or zero");
		}
		balance += amount;
	}
}
