# Sprint 4 — Wallet Management and Balance APIs

## Delivered scope

- `GET /api/v1/wallet` returns the authenticated user's wallet ID, two-decimal-place balance, currency, and lifecycle status.
- `GET /api/v1/wallet/balance` provides a smaller balance-and-currency representation for clients that do not need the complete wallet resource.
- Both endpoints require a valid bearer JWT. The controller resolves the `Jwt` principal with `@AuthenticationPrincipal`, takes its UUID `sub`, and delegates to `WalletService`.
- `WalletService` looks up the wallet with Spring Data's derived `findByUserId` method before mapping a DTO. The repository predicate is the ownership guarantee; no client-supplied `userId` or `walletId` participates in authorization.

## Status and error behavior

`ACTIVE`, `RESTRICTED`, and `CLOSED` wallets are readable. `RESTRICTED` is the current domain's blocked-equivalent status; it and `CLOSED` will deny money-changing operations in a later sprint, but remain available for balance visibility now. A missing wallet returns the standard `404 WALLET_NOT_FOUND` error body. Authentication failures continue to return the standard `401` response from Spring Security.

## Read and money semantics

The entity persists balance as `NUMERIC(19,2)` and the mapper normalizes its `BigDecimal` response to scale two without rounding. Sprint 4 deliberately has no balance mutation, payment, transfer, withdrawal, ledger, or multi-currency endpoint. `WalletService` read methods use `@Transactional(readOnly = true)` to express their non-mutating service boundary and permit provider optimizations where available.

## Verification

`WalletServiceTest` covers active wallet retrieval, no wallet, restricted/closed reads, and exact balance mapping. `WalletApiIntegrationTest` covers both APIs with valid JWTs, missing JWTs, the standard not-found error, lifecycle reads, generated OpenAPI paths, and the key isolation rule: User A's JWT returns Wallet A and never Wallet B.

The implementation follows Spring Security's documented [MVC principal resolution](https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html) and Spring Framework's [read-only transaction semantics](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html).
