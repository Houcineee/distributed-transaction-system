package com.houcinee.bankapp.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "customers")
public class Customer  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(nullable = false)
    private String name ;
    @NotNull
    @Column(nullable = false , unique = true)
    private String email;

    @OneToMany(mappedBy = "customer" , cascade = CascadeType.ALL , orphanRemoval = true)
    List<Account> accounts;
}
