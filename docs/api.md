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
