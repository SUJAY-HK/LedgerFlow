# Sprint 3 — Authentication and User Registration

## Delivered

- `POST /api/v1/auth/register` validates a request DTO, canonicalizes the email, rejects duplicates, hashes the password with BCrypt, creates the user, and creates a zero-balance INR wallet in one transaction.
- `POST /api/v1/auth/login` compares the raw password against the stored BCrypt hash and returns a signed bearer token on success.
- `GET /api/v1/users/me` is a small protected endpoint that reads the authenticated JWT principal and returns safe user data.
- JSON errors are centralized in `GlobalExceptionHandler`; bearer-token failures use the same response shape through a security entry point.

## JWT implementation

JWT consists of a header, payload, and signature. LedgerFlow issues HS256-signed tokens containing only `iss`, `sub` (the user's UUID), `iat`, and `exp`. A JWT payload is not encrypted, so it is not a place for credentials or other sensitive information.

The application uses Spring Security's OAuth 2.0 Resource Server integration. Its bearer-token filter extracts the `Authorization` header, verifies HS256 signatures and timestamps with the configured `JwtDecoder`, and creates the `SecurityContext` principal. No application `OncePerRequestFilter` is used. The security chain is stateless and auth endpoints are explicitly public.

`JWT_SECRET` is mandatory in non-test environments and must be at least 32 bytes for HS256. `JWT_EXPIRATION` defaults to `PT15M`, and `JWT_ISSUER` defaults to `ledgerflow`. Local `.env` files remain ignored by Git; no real secret is included in this repository.

## Swagger

OpenAPI documentation is available at `/swagger-ui/index.html` while the application is running. The `bearerAuth` security scheme makes Swagger UI's **Authorize** action available: paste the token value returned from login (Swagger adds the `Bearer` prefix) and call `GET /api/v1/users/me`.

## Verification

`./mvnw test` covers valid and duplicate registration, validation failure, BCrypt persistence, automatic wallet creation, a forced wallet-failure rollback, successful and failed login, JWT identity claims, and missing/invalid/valid bearer-token access to `/users/me`.

## Research references

- [Spring Security resource server JWT reference](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security UserDetailsService reference](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details-service.html)
- [Spring Security DaoAuthenticationProvider reference](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html)
