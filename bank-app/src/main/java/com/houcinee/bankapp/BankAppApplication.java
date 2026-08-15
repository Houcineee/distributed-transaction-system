package com.houcinee.bankapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class BankAppApplication {

  public static void main(String[] args) {
    SpringApplication.run(BankAppApplication.class, args);
  }
}
