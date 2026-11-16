package com.demobank.account;

/**
 * Record representing a single banking transaction.
 */
public record Transaction(String date, String description, double amount, double balance) {}
