# BlitzPay

## Generating key pairs

For instructions on generating the key pairs used by this project (public/private keys and signing requests), see the official TrueLayer documentation:

https://docs.truelayer.com/docs/sign-your-payments-requests

Follow the steps on that page to create and manage your keys; then place your private key (and any config) into the project as appropriate (for example, the repo contains example key files at the project root).

## Environment variables

The application expects the following environment variables to be set before running. Use the placeholders below and replace them with your actual credentials and key path.

- TRUELAYER_CLIENT_ID: TrueLayer client identifier
  - Example: `<YOUR_CLIENT_ID>`
- TRUELAYER_CLIENT_SECRET: TrueLayer client secret (sensitive)
  - Example: `<YOUR_CLIENT_SECRET>`
- TRUELAYER_KEY_ID: Key identifier used when signing requests
  - Example: `<YOUR_KEY_ID>`
- TRUELAYER_MERCHANT_ACCOUNT_ID: Merchant account ID
  - Example: `<YOUR_MERCHANT_ACCOUNT_ID>`
- TRUELAYER_PRIVATE_KEY_PATH: Path (relative to project root or absolute) to the private key file used for signing (PEM)
  - Example: `path/to/your-private-key.pem`

Note: Real credential values have been intentionally redacted in this README. Never commit secrets into the repository.

Suggested minimal `.env` file (do NOT commit this to version control):

```env
TRUELAYER_CLIENT_ID=<YOUR_CLIENT_ID>
TRUELAYER_CLIENT_SECRET=<YOUR_CLIENT_SECRET>
TRUELAYER_KEY_ID=<YOUR_KEY_ID>
TRUELAYER_MERCHANT_ACCOUNT_ID=<YOUR_MERCHANT_ACCOUNT_ID>
TRUELAYER_PRIVATE_KEY_PATH=path/to/your-private-key.pem
```

Example zsh commands to export variables for your current shell session and run the application (replace placeholders with real values locally):

```zsh
export TRUELAYER_CLIENT_ID="<YOUR_CLIENT_ID>"
export TRUELAYER_CLIENT_SECRET="<YOUR_CLIENT_SECRET>"
export TRUELAYER_KEY_ID="<YOUR_KEY_ID>"
export TRUELAYER_MERCHANT_ACCOUNT_ID="<YOUR_MERCHANT_ACCOUNT_ID>"
export TRUELAYER_PRIVATE_KEY_PATH="path/to/your-private-key.pem"

./gradlew bootRun
```

Or run with variables inline (single command):

```zsh
TRUELAYER_CLIENT_ID="<YOUR_CLIENT_ID>" \
TRUELAYER_CLIENT_SECRET="<YOUR_CLIENT_SECRET>" \
TRUELAYER_KEY_ID="<YOUR_KEY_ID>" \
TRUELAYER_MERCHANT_ACCOUNT_ID="<YOUR_MERCHANT_ACCOUNT_ID>" \
TRUELAYER_PRIVATE_KEY_PATH="path/to/your-private-key.pem" \
./gradlew bootRun
```

Security and repo hygiene notes
- Never commit private keys or `.env` files containing secrets into version control. Add entries to `.gitignore` such as:

```
# Ignore local environment files and private keys
.env
*.pem
```

- Prefer storing secrets in a secure vault or CI secret manager for production deployments.

If you'd like, I can also:
- add a `.env.example` file (non-sensitive) to the repo,
- add `.gitignore` entries for the private key and `.env`, or
- create a small Gradle task or startup script that loads the `.env` file securely.

## A2A Protocol support (native Embabel transport)

BlitzPay now uses Embabel's native remote transport modules for A2A and MCP instead of custom controller-level A2A endpoints.

### What was changed

- Added native Embabel modules:
  - `com.embabel.agent:embabel-agent-a2a`
  - `com.embabel.agent:embabel-agent-mcp`
- Added an `a2a` Spring profile (`application-a2a.yml`) that enables A2A and MCP transport support.
- Invoice goals already declared with `@AchievesGoal` in `InvoiceAgent` are now the goals exported remotely through Embabel when the profile is active.

### How to run with A2A + MCP enabled

```bash
SPRING_PROFILES_ACTIVE=a2a ./gradlew bootRun
```

You can combine with other profiles as needed, e.g. `SPRING_PROFILES_ACTIVE=a2a,dev`.

## Embabel version check

The project is currently pinned to `embabelAgentVersion=0.3.3`.

I attempted a dynamic latest-version check via Gradle dependency resolution, but repository metadata access is blocked in this execution environment (HTTP 403), so a safe, verified upgrade could not be completed automatically here.

