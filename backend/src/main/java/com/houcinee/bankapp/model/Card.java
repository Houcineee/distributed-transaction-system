package com.houcinee.bankapp.model;

import com.houcinee.bankapp.enums.CardStatus;
import com.houcinee.bankapp.enums.CardType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "cards")
public class Card extends BaseEntity {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@NotNull
	@Column(unique = true, nullable = false, length = 16)
	private String cardNumber;

	@NotNull
	@Column(nullable = false)
	private String cardholderName;

	@NotNull
	@Column(nullable = false, length = 3)
	private String cvv;

	@NotNull
	@Column(nullable = false)
	private LocalDate expirationDate;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CardType cardType;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CardStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	public Card(String cardNumber, String cardholderName, String cvv,
				LocalDate expirationDate, CardType cardType, Account account) {
		this.cardNumber = cardNumber;
		this.cardholderName = cardholderName;
		this.cvv = cvv;
		this.expirationDate = expirationDate;
		this.cardType = cardType;
		this.status = CardStatus.ACTIVE;
		this.account = account;
	}

	public void block() {
		this.status = CardStatus.BLOCKED;
	}

	public void activate() {
		if (this.status == CardStatus.EXPIRED || isExpired()) {
			throw new IllegalStateException("Cannot activate an expired card");
		}
		this.status = CardStatus.ACTIVE;
	}

	public boolean isExpired() {
		return this.status == CardStatus.EXPIRED
				|| (this.expirationDate != null && LocalDate.now().isAfter(this.expirationDate));
	}
}
