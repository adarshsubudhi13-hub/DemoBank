package com.demobank.account;

import java.util.List;

/**
 * Record representing a customer bank account.
 */
public record Account(String accountId, String owner, String maskedNumber, double balance, List<Transaction> transactions) {}
