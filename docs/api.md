# LedgerFlow API

All endpoints are prefixed with `/api/v1`. Request and response bodies are JSON.

## Registration

### `POST /api/v1/auth/register`

Registers a user and creates that user's INR wallet with a zero opening balance.

Request:

```json
{
  "name": "Sujay",
  "email": "sujay@example.com",
  "phone": "9876543210",
  "password": "SecurePassword123"
}
```

Response — `201 Created`:

```json
{
  "id": "44a3e5c8-2e58-4e25-a1be-8dc9f6c0c0e0",
  "name": "Sujay",
  "email": "sujay@example.com",
  "phone": "9876543210",
  "status": "ACTIVE"
}
```

Passwords and password hashes are never returned.

## Login

### `POST /api/v1/auth/login`

Request:

```json
{
  "email": "sujay@example.com",
  "password": "SecurePassword123"
}
```

Response — `200 OK`:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-14T12:30:00Z"
}
```

Invalid email and password combinations both produce the same `401` response.

## Current user

### `GET /api/v1/users/me`

Requires `Authorization: Bearer <accessToken>`.

Response — `200 OK`: the same safe user representation as registration. Missing, malformed, expired, or invalid tokens produce `401 Unauthorized`.

## Wallet

Wallet identity is derived from the bearer token's authenticated user. These current-user endpoints deliberately accept neither a `userId` nor a `walletId` from the client.

### `GET /api/v1/wallet`

Requires `Authorization: Bearer <accessToken>`.

Response — `200 OK`:

```json
{
  "walletId": "44a3e5c8-2e58-4e25-a1be-8dc9f6c0c0e0",
  "balance": 0.00,
  "currency": "INR",
  "status": "ACTIVE"
}
```

`401 Unauthorized` is returned for a missing, invalid, or expired token. `404 Not Found` means the authenticated user unexpectedly has no wallet. Wallets in `ACTIVE`, `RESTRICTED`, and `CLOSED` states remain readable in this sprint; status tells clients whether it is available for later money operations.

### `GET /api/v1/wallet/balance`

Requires `Authorization: Bearer <accessToken>`.

Response — `200 OK`:

```json
{
  "balance": 0.00,
  "currency": "INR"
}
```

It uses the same `401` and `404` behavior as `GET /api/v1/wallet`.

Both endpoints are intentional: `/wallet` is the evolving wallet resource representation, while `/wallet/balance` is a smaller, purpose-specific response for screens or jobs that only need a current monetary amount. This avoids coupling balance consumers to unrelated wallet fields while retaining a full resource endpoint.

## Transfers

### `POST /api/v1/transfers`

Requires `Authorization: Bearer <accessToken>`. The bearer token identifies the sender; the request deliberately contains no sender ID or wallet ID.

Request:

```json
{
  "recipientId": "44a3e5c8-2e58-4e25-a1be-8dc9f6c0c0e0",
  "amount": 500.00
}
```

Response — `201 Created`:

```json
{
  "transactionId": "b5045dca-cbd4-4c59-abef-a007dd960879",
  "recipientId": "44a3e5c8-2e58-4e25-a1be-8dc9f6c0c0e0",
  "amount": 500.00,
  "currency": "INR",
  "status": "COMPLETED",
  "createdAt": "2026-09-05T12:00:00Z"
}
```

Amounts must be positive and contain no more than two decimal places; values such as `500.123` are rejected rather than rounded. Sender and recipient must differ, both wallets must be `ACTIVE`, and the sender must have sufficient balance. Typical failures are `400 VALIDATION_FAILED` or `INVALID_TRANSFER`, `404 RECIPIENT_NOT_FOUND`, `409 WALLET_TRANSFER_NOT_ALLOWED`, and `422 INSUFFICIENT_BALANCE`. A failed transfer changes neither wallet and creates no transaction record.

### `GET /api/v1/transfers/{transactionId}`

Requires `Authorization: Bearer <accessToken>`. Returns `200 OK` with the transfer response above only when the authenticated user is its sender or recipient. All other callers receive `404 TRANSACTION_NOT_FOUND`, preventing object-ID guessing from disclosing another user's transfer.

## Errors

All application errors use this shape:

```json
{
  "timestamp": "2026-08-14T12:00:00Z",
  "status": 409,
  "error": "USER_ALREADY_EXISTS",
  "message": "An account with this email already exists",
  "path": "/api/v1/auth/register"
}
```

Validation errors additionally include a `fieldErrors` object mapping field names to messages. Typical status codes are `400` for malformed requests, `401` for failed authentication, `404` for an absent resource, and `409` for duplicate email addresses.
