package com.houcinee.bankapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "customers")
public class Customer extends BaseEntity {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@NotNull
	@Column(nullable = false)
	private String name;

	@NotNull
	@Column(nullable = false, unique = true)
	private String email;

	@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
	List<Account> accounts;

	public Customer(String name, String email) {
		this.name = name;
		this.email = email;
	}
}
