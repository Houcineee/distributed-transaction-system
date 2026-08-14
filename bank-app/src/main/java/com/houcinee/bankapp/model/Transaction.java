package com.houcinee.bankapp.model;

import com.houcinee.bankapp.enums.TransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "transactions")
public class Transaction extends BaseEntity {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@NotNull
	@Column(nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	private TransactionStatus status;

	@JoinColumn(name = "source_account_id", nullable = false)
	@ManyToOne(fetch = FetchType.LAZY)
	private Account sourceAccount;

	@JoinColumn(name = "destination_account_id", nullable = false)
	@ManyToOne(fetch = FetchType.LAZY)
	private Account destAccount;

	public Transaction(Long amount, Account sourceAccount, Account destAccount, TransactionStatus status) {
		this.amount = amount;
		this.sourceAccount = sourceAccount;
		this.destAccount = destAccount;
		this.status = status;
	}
}
