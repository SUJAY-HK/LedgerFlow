# Sprint 2 Notes

## What I implemented

- User entity with validation, UUID identity, status, password-hash storage, and audit timestamps.
- Wallet entity with INR-only currency, `BigDecimal` balance, status, and audit timestamps.
- Bidirectional User–Wallet one-to-one mapping, with a non-null unique wallet foreign key.
- Spring Data JPA repositories for users and wallets.
- Spring Data JPA auditing and H2-backed persistence tests for the important constraints.

## Problems I encountered

- The project did not have a test database, so repository tests would otherwise depend on the local PostgreSQL container. A test-scoped H2 database and test properties were added.
- The relational unique foreign key can enforce at most one wallet per user, but it cannot make a standalone user row impossible without a more complex schema design.

## What I learned

- `@Enumerated(EnumType.STRING)` protects stored status meaning from enum reordering.
- `BigDecimal` and a fixed `NUMERIC(19,2)` column avoid floating-point errors for INR values.
- JPA auditing uses entity listeners and application configuration rather than timestamp assignments scattered through business code.

## Things I still don't fully understand

- Whether a future account-deletion policy should retain wallets and transactions, soft-delete them, or use a different lifecycle aggregate.
- Whether future multi-currency support should introduce a dedicated currency/money value object instead of extending `WalletCurrency`.
