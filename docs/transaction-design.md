# Transfer and Transaction Design

## Model

A **wallet** is the current, mutable INR balance owned by one user. A **transfer** is the business operation that moves one positive amount from one wallet to another. A **transaction** is the persisted audit record of a successfully completed transfer. Sprint 5 intentionally does not implement a double-entry ledger; that is a later concern.

The transaction record holds its UUID, sender and recipient wallet relationships, amount, currency, status, and audit timestamps. Currency is retained because it makes the stored amount unambiguous if the product gains more currencies. There is no separate human reference: the UUID transaction ID is sufficient for this internal API. There is also no failure reason because failed synchronous requests are rolled back and do not create a transaction record.

## Lifecycle and invariants

The only persisted status in Sprint 5 is `COMPLETED`. A `PENDING` status would suggest asynchronous work that does not exist: debit, credit, and record creation happen in one synchronous database transaction. A failure instead rolls everything back; it is returned as an API error, not persisted as a failed transaction.

```text
Request → validate → authenticate sender → find recipient → validate wallets
→ validate amount → check balance → debit sender → credit recipient
→ persist transaction (COMPLETED) → COMMIT

Any failure → ROLLBACK
```

Transfer invariants:

- The JWT, never the request body, identifies the sender.
- Amount is positive, has at most two decimal places, and is stored as INR `NUMERIC(19,2)` without rounding.
- Sender and recipient differ.
- Both wallets must be `ACTIVE`; `RESTRICTED` and `CLOSED` wallets remain readable but cannot send or receive money.
- A sender balance can never become negative.
- A completed transfer debits and credits exactly the same amount and persists one transaction record atomically.
- A failure changes neither wallet and persists no transaction.

`Wallet.debit` and `Wallet.credit` own balance mutation rather than exposing an unrestricted setter. `debit` also enforces the non-negative balance invariant, so callers cannot bypass it accidentally.

## Transaction boundary and concurrency

`TransferService.transfer` is annotated with `@Transactional`. Both balance changes and `TransactionRepository.save` execute inside that boundary. Therefore, if transaction persistence (or any other step) throws a runtime exception, the database rolls back both managed wallet updates.

The race to avoid is two concurrent `₹800` transfers both reading a `₹1000` wallet and each deciding it has enough balance. The considered approaches were:

| Approach | Trade-off |
| --- | --- |
| Read, mutate Java object, save | Loses updates / can overdraw at normal isolation. |
| Optimistic locking | Can work with a version column and retry strategy, but contention failures must be surfaced or retried deliberately. |
| Atomic SQL update | Efficient for one balance predicate, but makes the paired credit and domain model less direct. |
| Pessimistic write lock | Serializes conflicting money operations at the database, letting the second request re-check the committed balance. |

Sprint 5 chooses JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)`. The service first obtains wallet IDs without loading wallet entities, then locks both rows in deterministic UUID order. The deterministic order also prevents the `A → B` / `B → A` deadlock pattern. Once the locks are held, it validates status and balance, mutates both domain entities, saves the transaction, and commits. This is intentionally database-backed coordination—not Redis or Kafka—and the concurrency integration test verifies that two simultaneous `₹800` transfers from a `₹1000` wallet yield exactly one success and a `₹200` balance.
