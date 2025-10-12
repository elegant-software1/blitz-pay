# OAuth2 and Keycloak Security Configuration

This document describes the OAuth2 Resource Server configuration integrated with Keycloak for securing the BlitzPay endpoints.

## Overview

The application now uses Spring Security OAuth2 Resource Server to secure endpoints with JWT tokens issued by Keycloak. This provides industry-standard OAuth2/OpenID Connect authentication.

## Components Added

### 1. Dependencies
- `spring-boot-starter-oauth2-resource-server`: Provides OAuth2 Resource Server support with JWT validation

### 2. Security Configuration (`SecurityConfig.kt`)

The `SecurityConfig` class configures:

- **CSRF Protection**: Disabled for REST API (stateless)
- **Session Management**: Stateless (no server-side sessions)
- **Authorization Rules**:
  - `/webhooks/**` - Publicly accessible (uses custom signature verification)
  - `/actuator/**` - Publicly accessible (for health checks and metrics)
  - `/swagger-ui/**`, `/v3/api-docs/**` - Publicly accessible (API documentation)
  - All other endpoints - Require authentication with JWT token

### 3. Application Configuration (`application.yml`)

OAuth2 Resource Server settings:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/quickpay}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8080/realms/quickpay/protocol/openid-connect/certs}
```

### 4. OpenAPI/Swagger Integration (`OpenApiConfig.kt`)

Updated to include OAuth2 security schemes:
- **keycloak_oauth2**: OAuth2 Authorization Code flow
- **bearer_jwt**: Bearer token authentication

## Environment Variables

Set these environment variables to configure Keycloak integration:

- `KEYCLOAK_ISSUER_URI`: The Keycloak realm issuer URI (e.g., `http://localhost:8080/realms/quickpay`)
- `KEYCLOAK_JWK_SET_URI`: The JWKS endpoint for JWT verification (e.g., `http://localhost:8080/realms/quickpay/protocol/openid-connect/certs`)

## Keycloak Setup

### Prerequisites
1. Running Keycloak instance (local or remote)
2. A realm created (e.g., `quickpay`)
3. Client configured for the application

### Keycloak Configuration Steps

1. **Create a Realm**:
   - Login to Keycloak Admin Console
   - Create a new realm named `quickpay`

2. **Create a Client**:
   - Go to Clients → Create
   - Client ID: `quickpay-api`
   - Client Protocol: `openid-connect`
   - Access Type: `public` (for frontend) or `confidential` (for backend)
   - Valid Redirect URIs: `http://localhost:8080/*`
   - Web Origins: `http://localhost:8080`

3. **Create Roles** (optional):
   - Define roles like `payment-user`, `payment-admin`
   - Assign roles to users

4. **Create Users**:
   - Go to Users → Add User
   - Set username, email, etc.
   - Set credentials in the Credentials tab
   - Assign roles in the Role Mappings tab

## Testing Authentication

### 1. Obtain Access Token

Using Keycloak's token endpoint:

```bash
curl -X POST http://localhost:8080/realms/quickpay/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=quickpay-api" \
  -d "username=testuser" \
  -d "password=testpass"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

### 2. Access Protected Endpoint

Use the access token to call protected endpoints:

```bash
curl -X POST http://localhost:8080/payments/request \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER-123",
    "amountMinorUnits": 1000,
    "currency": "GBP",
    "userDisplayName": "Test User",
    "redirectReturnUri": "http://example.com/return"
  }'
```

### 3. Access Public Endpoints

Webhook and documentation endpoints remain public:

```bash
# Swagger UI (no token required)
curl http://localhost:8080/swagger-ui/index.html

# Health check (no token required)
curl http://localhost:8080/actuator/health

# Webhook endpoint (no token required, uses custom signature verification)
curl -X POST http://localhost:8080/webhooks/truelayer \
  -H "Content-Type: application/json" \
  -H "tl-signature: <signature>" \
  -d '{...}'
```

## Endpoint Security Summary

| Endpoint Pattern | Security | Notes |
|-----------------|----------|-------|
| `/payments/**` | OAuth2 JWT Required | Protected payment endpoints |
| `/qr-payments/**` | OAuth2 JWT Required | Protected QR payment endpoints |
| `/webhooks/**` | Public | Uses custom TrueLayer signature verification |
| `/actuator/**` | Public | Health checks and metrics |
| `/swagger-ui/**` | Public | API documentation |
| `/v3/api-docs/**` | Public | OpenAPI specification |

## Swagger UI Authentication

The Swagger UI now includes authentication options:

1. Navigate to: `http://localhost:8080/swagger-ui/index.html`
2. Click the "Authorize" button
3. Choose either:
   - **keycloak_oauth2**: Redirect to Keycloak login (OAuth2 Authorization Code flow)
   - **bearer_jwt**: Enter JWT token directly

## Token Validation

The application validates JWT tokens by:

1. Fetching public keys from Keycloak's JWKS endpoint
2. Verifying the token signature using the public key
3. Validating token claims (issuer, expiration, etc.)
4. Extracting user information from token claims

## Security Best Practices

1. **HTTPS**: Use HTTPS in production to protect tokens in transit
2. **Token Expiration**: Configure appropriate token expiration times in Keycloak
3. **Refresh Tokens**: Use refresh tokens to obtain new access tokens
4. **Scope/Role-based Access**: Implement role-based access control using JWT claims
5. **Token Storage**: Store tokens securely on the client side (e.g., HttpOnly cookies, secure storage)

## Troubleshooting

### Common Issues

1. **401 Unauthorized on all requests**
   - Verify Keycloak is running and accessible
   - Check `KEYCLOAK_ISSUER_URI` and `KEYCLOAK_JWK_SET_URI` configuration
   - Ensure token is included in `Authorization: Bearer <token>` header

2. **Token validation fails**
   - Verify token is not expired
   - Check that `issuer-uri` matches the token's `iss` claim
   - Ensure JWKS endpoint is accessible from the application

3. **Webhook endpoint returns 401**
   - This should NOT happen - webhook endpoints are public
   - Check SecurityConfig for proper `/webhooks/**` pattern matching

## Development Mode

For local development without Keycloak:

1. Comment out or remove the security configuration temporarily
2. Or set up a local Keycloak instance using Docker:

```bash
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

## Production Considerations

1. **Environment Variables**: Configure production Keycloak URLs via environment variables
2. **HTTPS**: Enable HTTPS for both application and Keycloak
3. **Network Security**: Ensure Keycloak JWKS endpoint is accessible from application
4. **Monitoring**: Monitor authentication failures and token validation errors
5. **Rate Limiting**: Consider rate limiting authentication endpoints

## Additional Resources

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OpenID Connect](https://openid.net/connect/)
- [JWT.io](https://jwt.io/) - JWT token decoder

## Migration Notes

Existing applications/clients must be updated to:

1. Obtain JWT tokens from Keycloak before making API calls
2. Include tokens in the `Authorization: Bearer <token>` header
3. Handle token expiration and refresh scenarios
4. Update any automated tests to include authentication
