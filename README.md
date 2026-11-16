# DemoBank — Fake Online Bank & Victim Target

DemoBank is a lightweight, zero-framework fake online bank serving as the victim target for **Sentinel**. It provides a realistic banking login portal, customer account dashboard, append-only authentication audit logging, and administrative defense controls.

---

## 🚀 Running DemoBank

### Build and Package
```bash
cd demobank
mvn clean package
```

### Launch Server
```bash
java -jar target/demobank.jar
```
Binds strictly to `http://127.0.0.1:9090`.

---

## 🔑 Seed User Credentials

Loaded automatically on first startup into `data/users.csv`:

| Username | Password | Full Name | Account ID | Initial Balance |
| :--- | :--- | :--- | :--- | :--- |
| `alice` | `Wonder@2024` | Alice Johnson | `ACC-1001` | $14,850.75 |
| `bob` | `Builder#77` | Bob Smith | `ACC-1002` | $3,420.50 |
| `charlie` | `Choc0late!` | Charlie Brown | `ACC-1003` | $8,910.00 |
| `admin` | `Adm1n$ecure` | Admin User | `ACC-1004` | $1,250,000.00 |
| `guest` | `guest123` | Guest User | `ACC-1005` | $500.00 |

*Note on Hashing:* DemoBank uses SHA-256 with per-user salt. In production, password hashing must use memory-hard functions such as Argon2id or bcrypt.

---

## 🌐 Endpoints & API Reference

| Method | Path | Authentication | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | None | Bank login web page |
| `GET` | `/dashboard` | Session cookie | Customer banking account summary & transaction ledger |
| `GET` | `/status.html` | None | Live visual telemetry status page |
| `POST` | `/api/login` | None | Authenticate with `{username, password}`; sets `SID` cookie |
| `POST` | `/api/logout` | Session cookie | Invalidate active user session |
| `GET` | `/api/account` | Session cookie | JSON account & transaction history |
| `GET` | `/api/stats` | None | Public authentication counters (`attempts`, `failures`, `blockedIps`, `lockedUsers`) |
| `GET` | `/health` | None | Server health check (`{"status":"ok"}`) |
| `POST` | `/admin/block` | `X-Admin-Token` + localhost | Block specified IP `{ip}` |
| `POST` | `/admin/unblock` | `X-Admin-Token` + localhost | Unblock specified IP `{ip}` |
| `POST` | `/admin/lock-user` | `X-Admin-Token` + localhost | Temporarily lock username `{username, seconds}` (1–3600s) |
| `POST` | `/admin/unlock-user`| `X-Admin-Token` + localhost | Unlock username `{username}` |
| `GET` | `/admin/status` | `X-Admin-Token` + localhost | Comprehensive telemetry & current security state |
| `POST` | `/admin/reset` | `X-Admin-Token` + localhost | Clear blocks/locks/counters and rotate `auth.log` to `auth.log.bak` |

---

## 📜 Sentinel Integration Contract

### Client IP Resolution
DemoBank inspects the first valid IPv4 address in the `X-Forwarded-For` header. If absent or invalid, it falls back to the socket remote address.
> **Security Notice:** Trusting client-supplied `X-Forwarded-For` headers is strictly acceptable for this local simulation demo to allow Sentinel to simulate distributed attack IP ranges (`203.0.113.x`, `198.51.100.x`). In real-world deployments, client IP spoofing must be guarded by verified reverse-proxy hops.

### Audit Log Contract (`data/auth.log`)
Each authentication attempt writes one append-only UTF-8 JSON line:
```json
{"ts":"2026-11-09T10:15:30.123Z","ip":"203.0.113.45","username":"admin","success":false,"reason":"BAD_PASSWORD","status":401}
```
- Reason codes: `NONE`, `BAD_PASSWORD`, `UNKNOWN_USER`, `IP_BLOCKED`, `ACCOUNT_LOCKED`.
- Passwords are never written to disk or logs.
- Flushed immediately so Sentinel's `LogTailer` can ingest within 100 ms.

---

## ⚖️ Architectural Decisions
1. **Thread-Safe Single-Writer Logging**: To prevent log line corruption during high-throughput attack floods, all HTTP worker threads place entries onto a `LinkedBlockingQueue`, which is drained and written by a single dedicated daemon thread using a buffered append-mode writer.
2. **Lazy Account Lock Expiration**: Rather than scheduling background timers for every temporary user lock, expirations are evaluated lazily on each access with instantaneous eviction of stale records.
3. **Strict Localhost Admin Protection**: Admin APIs require both the pre-shared `X-Admin-Token` header and verification that the caller's socket IP is loopback (`127.0.0.1`).
