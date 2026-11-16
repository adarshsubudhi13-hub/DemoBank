package com.demobank.account;

import com.demobank.config.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository providing account details and transactions backed by accounts.json.
 */
public final class AccountRepository {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String jsonPath;

    public AccountRepository() {
        this(Config.getAccountsPath());
    }

    public AccountRepository(String jsonPath) {
        this.jsonPath = jsonPath;
        initAccounts();
    }

    public Optional<Account> findById(String accountId) {
        if (accountId == null) return Optional.empty();
        return Optional.ofNullable(accounts.get(accountId));
    }

    private void initAccounts() {
        Path path = Paths.get(jsonPath);
        if (!Files.exists(path)) {
            seedDefaults(path);
        }
        loadFromJson(path);
    }

    private void seedDefaults(Path path) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            List<Account> list = List.of(
                buildAccount("ACC-1001", "Alice Johnson", "**** **** **** 4821", 14850.75),
                buildAccount("ACC-1002", "Bob Smith", "**** **** **** 9102", 3420.50),
                buildAccount("ACC-1003", "Charlie Brown", "**** **** **** 3341", 8910.00),
                buildAccount("ACC-1004", "Admin User", "**** **** **** 0001", 1250000.00),
                buildAccount("ACC-1005", "Guest User", "**** **** **** 7789", 500.00),
                buildAccount("ACC-1006", "Adarsh Subudhi", "**** **** **** 1313", 50000.00)
            );
            Files.writeString(path, gson.toJson(list));
        } catch (IOException e) {
            System.err.println("Failed to seed accounts.json: " + e.getMessage());
        }
    }

    private Account buildAccount(String id, String owner, String masked, double initialBal) {
        List<Transaction> txs = List.of(
            new Transaction("2026-11-01", "Direct Deposit - Salary", 4500.00, initialBal),
            new Transaction("2026-11-02", "Grocery Store #104", -124.50, initialBal - 124.50),
            new Transaction("2026-11-03", "Utility Bill - Electric", -85.20, initialBal - 209.70),
            new Transaction("2026-11-04", "Coffee & Bakery", -14.50, initialBal - 224.20),
            new Transaction("2026-11-05", "Online Marketplace", -65.90, initialBal - 290.10),
            new Transaction("2026-11-06", "ATM Withdrawal", -100.00, initialBal - 390.10),
            new Transaction("2026-11-07", "Dining Out", -54.30, initialBal - 444.40),
            new Transaction("2026-11-08", "Mobile Carrier Bill", -45.00, initialBal - 489.40),
            new Transaction("2026-11-09", "Interest Earned", 12.50, initialBal - 476.90)
        );
        return new Account(id, owner, masked, initialBal, txs);
    }

    private void loadFromJson(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<Account>>(){}.getType();
            List<Account> list = gson.fromJson(reader, listType);
            if (list != null) {
                for (Account acc : list) {
                    accounts.put(acc.accountId(), acc);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load accounts.json: " + e.getMessage());
        }
    }
}
