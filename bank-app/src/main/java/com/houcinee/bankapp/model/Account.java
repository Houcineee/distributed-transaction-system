package com.houcinee.bankapp.model;

import com.houcinee.bankapp.enums.Currency;
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
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Currency currency;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser appUser;

	@OneToMany(mappedBy = "sourceAccount")
	private List<Transaction> sourceTransactions;

	@OneToMany(mappedBy = "destAccount")
	private List<Transaction> destinationTransactions;

	public Account(Long balance, Currency currency, AppUser appUser) {
		this.balance = balance;
		this.currency = currency;
		this.appUser = appUser;
	}

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
