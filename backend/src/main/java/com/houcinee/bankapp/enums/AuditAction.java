package com.houcinee.bankapp.enums;

public enum AuditAction {
    // Auth & Access
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    ACCESS_DENIED,

    // Customer
    USER_REGISTERED,
    CUSTOMER_UPDATED,
    CUSTOMER_DELETED,

    // Account
    ACCOUNT_CREATED,
    ACCOUNT_UPDATED,
    ACCOUNT_CLOSED,

    // Financial
    TRANSFER_EXECUTED,
    TRANSFER_FAILED,

    // Card
    CARD_CREATED,
    CARD_BLOCKED,
    CARD_ACTIVATED
}
