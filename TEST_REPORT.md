# DemoBank — Automated Test Execution Report

Execution Date: 2026-11-09  
Environment: Windows 11, OpenJDK 64-Bit Server VM (build 21.0.6), Apache Maven 3.9  
Framework: JUnit 5 (Jupiter 5.10.2)

---

## 📊 Test Execution Summary

| Test Suite | Tests Run | Failures | Errors | Result |
| :--- | :--- | :--- | :--- | :--- |
| `com.demobank.PasswordHasherTest` | 3 | 0 | 0 | **PASS** |
| `com.demobank.SecurityStateTest` | 4 | 0 | 0 | **PASS** |
| `com.demobank.AuthLoggerTest` | 1 | 0 | 0 | **PASS** |
| `com.demobank.AuthServiceTest` | 1 | 0 | 0 | **PASS** |
| `com.demobank.SessionTest` | 2 | 0 | 0 | **PASS** |
| `com.demobank.LoginApiTest` | 1 | 0 | 0 | **PASS** |
| `com.demobank.AdminApiTest` | 1 | 0 | 0 | **PASS** |
| `com.demobank.ConcurrencyTest` | 1 | 0 | 0 | **PASS** |
| `com.demobank.LoadTest` | 1 | 0 | 0 | **PASS** |
| **Total** | **15** | **0** | **0** | **ALL GREEN (100% PASS)** |

---

## 🔍 Detailed Test Cases & Expectations

### 1. `PasswordHasherTest`
- `testSamePasswordAndSaltYieldSameHash`: Identical password and salt generate deterministic 64-char hex SHA-256 output. **[PASS]**
- `testDifferentSaltsYieldDifferentHashes`: Different salts yield distinct hashes for the same password. **[PASS]**
- `testNullArgsThrowException`: Null arguments trigger `IllegalArgumentException`. **[PASS]**

### 2. `SecurityStateTest`
- `testBlockAndUnblock`: Adding and removing IP addresses from blocked list functions idempotently. **[PASS]**
- `testLockAndLazyExpiry`: Temporary account lock active immediately, expires after specified duration. **[PASS]**
- `testUnlockUser`: Manual unlock clears locked status. **[PASS]**
- `testReset`: Clearing state removes all blocks and active locks. **[PASS]**

### 3. `AuthLoggerTest`
- `testLinesAreValidJsonAndInOrder`: Background queue drainage writes formatted JSON lines sequentially with immediate flush. **[PASS]**

### 4. `AuthServiceTest`
- `testCheckOrderAndAllFiveOutcomes`: Verifies strict evaluation precedence:
  1. `IP_BLOCKED` (403)
  2. `UNKNOWN_USER` (401)
  3. `ACCOUNT_LOCKED` (423)
  4. `BAD_PASSWORD` (401)
  5. `NONE` success (200 + token)
  6. Malformed input (400, unlogged) **[PASS]**

### 5. `SessionTest`
- `testSessionLifecycle`: Session creation generates 32-hex token, retrieves username, and revokes on invalidation. **[PASS]**
- `testInvalidTokenReturnsEmpty`: Unknown or null tokens safely return empty. **[PASS]**

### 6. `LoginApiTest`
- `testAllLoginPathsAndExactLogLines`: End-to-end HTTP requests on ephemeral port testing 200, 401, 403, 423, and 400 responses, verifying exact corresponding lines in `auth.log`. **[PASS]**

### 7. `AdminApiTest`
- `testTokenAuthenticationAndAdminActions`: Enforces `X-Admin-Token` authentication, tests block, lock, unblock, and `/admin/reset` log rotation. **[PASS]**

### 8. `ConcurrencyTest`
- `test20ThreadsX500LoginsExactCountersAndValidJson`: 20 concurrent threads execute 500 logins each (10,000 total operations). Confirmed exact matching counters in memory and 10,000 valid, uncorrupted JSON lines in `auth.log`. **[PASS]**

### 9. `LoadTest`
- `testHighThroughput200RpsBenchmark`: High-throughput stress test verified sustained throughput far exceeding the 200 requests/second requirement with zero unhandled exceptions or lost lines. **[PASS]**
