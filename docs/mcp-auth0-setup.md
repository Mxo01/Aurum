# MCP authorization on Auth0

The backend is an OAuth 2.1 **resource server**. Auth0 is the **authorization
server**. There is no OAuth proxy in the backend any more: the old `/oauth/*`
endpoints are gone. The MCP transport is the stateless profile of Spring AI
2.0 / MCP Java SDK 2.1 (protocol 2025-11-25).

```
MCP client ──POST /mcp──────────────▶ 401 + WWW-Authenticate: resource_metadata=…
           ◀────────────────────────
           ──GET /.well-known/oauth-protected-resource/mcp──▶ {resource, authorization_servers}
           ──GET https://aurum-api.eu.auth0.com/.well-known/oauth-authorization-server──▶ endpoints
           ──authorize + PKCE + resource=https://api.aurum-networth.com/mcp──▶ Auth0 login
           ◀──code──  ──token exchange──▶ access token (aud = the MCP API)
           ──POST /mcp + Bearer token──▶ tools
```

What the backend enforces:

- `iss` is the configured Auth0 issuer and `aud` contains `mcp.audience`
  (defaults to `mcp.resource-url`). A token minted for the web app is rejected.
- The MCP transport is stateless: every request is authenticated, and there is
  no session id to steal or replay.
- Browser origins are rejected unless listed in `MCP_ALLOWED_ORIGINS`; native
  MCP clients send no `Origin` and are unaffected.
- If `MCP_READ_SCOPES` and `MCP_WRITE_SCOPES` are configured, tools whose name
  starts with `get` require a read scope and every other tool requires a write
  scope. Unknown tool names are treated as writes.

## 1. Create the API

Auth0 Dashboard → **Applications → APIs → Create API**:

| Field             | Value                                |
| ----------------- | ------------------------------------ |
| Name              | `Aurum MCP`                          |
| Identifier        | `https://api.aurum-networth.com/mcp` |
| Signing Algorithm | `RS256`                              |

The identifier is the canonical MCP resource URI: the same string clients put in
the RFC 8707 `resource` parameter and the backend expects in `aud`. Do not use a
trailing slash.

On the **Permissions** tab add:

| Permission  | Description                                                |
| ----------- | ---------------------------------------------------------- |
| `mcp:read`  | Read assets, snapshots, targets and cash flow              |
| `mcp:write` | Create and update assets, snapshots, targets and cash flow |

## 2. Enable the MCP tenant settings

Settings → **Advanced**:

- **Resource Parameter Compatibility Profile** ON
- **Include Issuer in Authorization Responses** ON
- **Dynamic Client Registration** ON (only if your MCP client uses DCR; see below)
- **Enable Application Connections** ON (DCR clients need the login connection)

Set **DCR Security Mode** to **Strict**.

These are the same switches as:

```http
PATCH https://aurum-api.eu.auth0.com/api/v2/tenants/settings
Authorization: Bearer $AUTH0_MGMT_TOKEN
Content-Type: application/json

{
  "resource_parameter_profile": "compatibility",
  "authorization_response_iss_parameter_supported": true,
  "flags": {
    "enable_dynamic_client_registration": true,
    "enable_client_connections": true
  }
}
```

`compatibility` is the value that makes Auth0 fall back to the RFC 8707
`resource` parameter when `audience` is absent. The `audience` parameter keeps
precedence, which is what the web app still uses.

## 3. Grant the MCP API to third-party clients

Dynamically registered clients are third-party applications and need an explicit
grant. Give them the least privilege you need: read-only if you only query,
read and write if the agent may modify data.

Dashboard → APIs → **Aurum MCP** → **Settings** → **Default Permissions for
Third-Party Applications** → **User-Delegated Access** → **Authorized** → select
`mcp:read` (and `mcp:write` if needed).

Management API equivalent:

```http
POST https://aurum-api.eu.auth0.com/api/v2/client-grants
Authorization: Bearer $AUTH0_MGMT_TOKEN
Content-Type: application/json

{
  "default_for": "third_party_clients",
  "audience": "https://api.aurum-networth.com/mcp",
  "scope": ["mcp:read", "mcp:write"],
  "subject_type": "user"
}
```

If a default grant already exists for this audience, update it instead of
creating a second one.

**Pre-registered clients (no DCR).** Create a **Native** application, allow the
authorization code grant, enable refresh tokens, and authorize it for the MCP
API with the same scopes. Then configure the client with its `client_id`
instead of letting it register. This is the safer option if you do not want an
open registration endpoint.

## 4. Configure the backend

In production the values are read from **GitHub Actions repository
variables**; the deploy workflow writes them into `/opt/aurum/.env` before
`docker compose up`:

```bash
gh variable set MCP_RESOURCE_URL --body "https://api.aurum-networth.com/mcp"
gh variable set MCP_AUDIENCE --body "https://api.aurum-networth.com/mcp"
gh variable set MCP_READ_SCOPES --body "mcp:read"
gh variable set MCP_WRITE_SCOPES --body "mcp:write"
# MCP_ALLOWED_ORIGINS is optional and empty by default
```

For local development use the same keys in `application-local.properties`:

```dotenv
MCP_RESOURCE_URL=https://api.aurum-networth.com/mcp
MCP_AUDIENCE=https://api.aurum-networth.com/mcp
MCP_READ_SCOPES=mcp:read
MCP_WRITE_SCOPES=mcp:write
MCP_ALLOWED_ORIGINS=
MCP_MAX_BATCH_SIZE=500
```

- `MCP_RESOURCE_URL` is the canonical resource URI and must match the API
  identifier exactly.
- `MCP_AUDIENCE` is optional and defaults to `MCP_RESOURCE_URL`. Set it only if
  the API identifier is different from the resource URI.
- `MCP_READ_SCOPES` and `MCP_WRITE_SCOPES` must be set together or left both
  empty. Leaving both empty enforces audience binding only.
- `MCP_ALLOWED_ORIGINS` is a comma-separated allowlist of browser origins. The
  default (empty) blocks every browser origin. Native clients do not send
  `Origin`.
- Remove `MCP_AUTH0_CLIENT_ID`: the backend no longer registers clients or
  proxies tokens.

## 5. Deploy order

1. Apply the Auth0 changes above.
2. Add the environment variables on the server.
3. Merge and deploy the backend.

The deploy workflow refuses to start if `MCP_RESOURCE_URL` is not an
`https://…/mcp` URL. It also removes the obsolete `MCP_AUTH0_CLIENT_ID` line. If you deploy before the Auth0 API exists, the backend still
starts but `/mcp` rejects every token (the audience was never issued) — fail
closed, not open.

## 6. Verify

```bash
# Protected Resource Metadata, both discovery paths
curl -s https://api.aurum-networth.com/.well-known/oauth-protected-resource/mcp | jq
curl -s https://api.aurum-networth.com/.well-known/oauth-protected-resource | jq

# Auth0 discovery: registration_endpoint appears only when DCR is enabled
curl -s https://aurum-api.eu.auth0.com/.well-known/oauth-authorization-server \
  | jq '{issuer, registration_endpoint, code_challenge_methods_supported}'

# Unauthenticated MCP request: 401 + challenge
curl -si -X POST https://api.aurum-networth.com/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"probe","version":"0"}}}'
```

The 401 must contain:

```http
WWW-Authenticate: Bearer realm="aurum-mcp", resource_metadata="https://api.aurum-networth.com/.well-known/oauth-protected-resource/mcp", scope="mcp:read mcp:write"
```

Then point the MCP client at `https://api.aurum-networth.com/mcp`. On first
connection it discovers the metadata, registers (or uses its pre-registered
client), opens the browser, and receives a token with
`aud=https://api.aurum-networth.com/mcp`.

## Local development

Create a second Auth0 API whose identifier is `http://localhost:8080/mcp`, or
set `MCP_RESOURCE_URL` to an identifier that exists in the tenant. The local
`application-local.properties` in this repository points at
`http://localhost:8080/mcp`.

## Rollback

The old proxy is deleted, not feature-flagged. To roll back, revert the commit
and redeploy; the Auth0 API and tenant settings can stay, they are additive.

## Why the old flow was removed

The previous implementation was a custom OAuth proxy. It accepted an arbitrary
`redirect_uri` on `/oauth/authorize` and forwarded the resulting authorization
code to it, which allowed an attacker to receive another user's code and
exchange it for a token. It also proxied the token endpoint without pinning a
client, accepted any grant type, advertised the backend itself as the
authorization server, and shared the web app's audience. Since Auth0 now
supports the `resource` parameter, PRM, AS metadata and DCR natively, the proxy
has no reason to exist.
