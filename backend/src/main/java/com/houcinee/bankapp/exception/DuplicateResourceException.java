package com.houcinee.bankapp.exception;

public class DuplicateResourceException extends BankingException{
	public DuplicateResourceException(String msg){
		super(msg) ;
	}
}
