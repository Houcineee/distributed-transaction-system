package com.houcinee.bankapp.model;

import com.houcinee.bankapp.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "customers")
public class Customer extends BaseEntity implements UserDetails {
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

	@NotEmpty
	@Column(nullable = false  )
	private String password;

	@NotNull
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	private Role role;


	@Override
	public String getUsername() {
		return this.email;
	}

	@Column(nullable = false)
	private boolean accountLocked = false;

	@Column(nullable = false)
	private boolean enabled = true;


	@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
	List<Account> accounts;

	public Customer(String name, String email, String password, Role role) {
		this.name = name;
		this.email = email;
		this.password = password;
		this.role = role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return this.password;
	}


	@Override
	public boolean isAccountNonLocked() {
		return !this.accountLocked;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}
}
