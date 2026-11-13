# Security audit — November 2026

Scope: `aurum-be` (Spring Boot API and MCP server) and `aurum-fe` (Angular
SPA). Focus: the MCP authorization layer, which was written before the MCP
authorization spec was adopted and had grown a custom OAuth proxy.

Status key: **fixed** in the `fix/mcp-native-auth` branch, **open** as a
follow-up, **info** for accepted risk or context.

## Critical and high

### 1. Authorization code theft through the OAuth proxy — fixed

`McpOAuthController#authorize` stored the caller's `redirect_uri` keyed by
`state`, replaced it with the backend callback, and then `#callback` forwarded
Auth0's authorization code to the stored URI without validating it.

An unauthenticated attacker could:

1. call `/oauth/register` (or reuse the public MCP client id) to get a
   `client_id`;
2. craft `/oauth/authorize?redirect_uri=https://attacker.example/cb&state=…`
   with their own PKCE pair and send the link to a victim;
3. receive the victim's `code` at `attacker.example`;
4. exchange it at `/oauth/token` and obtain a token for the victim's account.

The proxy was removed. Auth0 is now the authorization server, PKCE and the
redirect URI are enforced by Auth0, and the backend only validates tokens.

### 2. Open token/DCR proxy — fixed

`/oauth/token` forwarded any grant type, any `client_id`, and any
`client_secret` to Auth0; `/oauth/register` echoed arbitrary redirect URIs and
returned a fixed client id. This was an open proxy to the tenant and allowed
audience injection for any Auth0 client. Removed with the proxy.

### 3. Web-app tokens were valid on the MCP endpoint — fixed

The MCP filter chain validated the same `aud` as the SPA (`AUTH0_AUDIENCE`), so
any token minted for the web app could call every MCP tool. The MCP chain now
validates `mcp.audience` (defaulting to the MCP resource URI, which is the
Auth0 API identifier), per RFC 8707/RFC 9728. A web-app token is rejected.

### 4. IDOR on target update — fixed

`TargetService#update` loaded a target by id and never compared its owner with
the authenticated user. `TargetFacade#updateTarget` and the MCP
`updateTarget`/`bulkUpdateTargets` tools reached it, so any authenticated user
who knew a target UUID could rewrite another user's goal.

`TargetService#update` now takes `userId`, and `findOwnedTarget` throws
`AccessDeniedException` for a mismatch. `delete` uses the same check.

### 5. Non-compliant discovery documents — fixed

The backend advertised itself as the authorization server
(`authorization_servers: [resourceUrl]`, `issuer: resourceUrl`) while Auth0
issued the tokens, and the protected-resource metadata used the host root
instead of the MCP endpoint. MCP clients that validate `iss` (RFC 9207) or the
metadata `resource` (RFC 9728) could not use it.

The backend now publishes only the resource metadata:
`resource` is the canonical MCP URI, `authorization_servers` is the Auth0
issuer, and the document is served at both
`/.well-known/oauth-protected-resource` and
`/.well-known/oauth-protected-resource/mcp`. Authorization-server metadata is
served by Auth0.

## Medium

### 6. MCP CORS allowed every origin with credentials — fixed

`mcpCorsConfigurationSource` used `allowedOriginPatterns("*")` with
`allowCredentials(true)`, which reflects any `Origin`. `/mcp` is now limited to
`MCP_ALLOWED_ORIGINS`, credentials are disabled, and a filter rejects requests
whose `Origin` is not allowlisted (native clients send no `Origin`). An empty
allowlist blocks all browser origins.

### 7. Unbounded OAuth state map — fixed by removal

`ConcurrentHashMap<String, PendingCallback>` had no size cap and was only
pruned every five minutes; an attacker could grow it without bound. The map no
longer exists.

### 8. No read/write separation — fixed (optional)

Every MCP tool was available to any valid token, including the destructive bulk
tools. When `MCP_READ_SCOPES`/`MCP_WRITE_SCOPES` are configured, `get*` tools
require the read scope and every other tool requires the write scope; unknown
tool names are treated as writes.

### 9. Unbounded MCP bulk operations — fixed

`bulkAdd*`/`bulkUpdate*` accepted arbitrary list sizes, so one tool call could
allocate and persist unbounded data. They now reject lists over
`mcp.max-batch-size` (default 500) and null lists.

### 10. Image upload decompression bomb — fixed

A 5 MB file was decoded before any dimension check, so a crafted image with
hundreds of megapixels could exhaust the heap. Dimensions are now read from the
image header and images over 40 megapixels are rejected before decoding.

### 11. Tokens in `localStorage` — open

`auth0Config.cacheLocation` is `localstorage` and refresh tokens are enabled, so
an XSS can exfiltrate a long-lived refresh token. The chart tooltips that use
`innerHTML` escape user-controlled labels, so this is not currently reachable
through asset names, but the storage choice remains the largest XSS blast
radius in the app. Moving to `memory` (with silent-auth/refresh fallback) is a
UX trade-off that deserves its own change.

### 12. End-of-life framework — open

`spring-boot-starter-parent` is 3.4.4 and Spring AI is 1.1.3 (MCP Java SDK
0.17, protocol 2025-06-18). Spring Boot 3.4 is out of OSS support and the MCP
SDK is two protocol revisions behind. A separate branch will move to Spring
Boot 4.1.x + Spring AI 2.0.x (MCP SDK 2.1, protocol 2025-11-25) and re-run the
suite.

### 13. CI supply chain — open

Actions are pinned to mutable tags (`actions/checkout@v4`,
`docker/build-push-action@v6`, `appleboy/*@v…`) and the deploy image is only
tagged `latest`, so there is no digest pinning and no immutable rollback
artifact. Pinning every action to a commit SHA and tagging images with the
commit SHA is the fix.

### 14. gitleaks allowlist was too broad — fixed

`aurum-be/src/test/.*\.java$` and `.*\.spec\.ts$` were allowlisted wholesale,
so any real secret committed in a test would have been ignored. The allowlist
now names the three fixture files that actually need it; the full-history scan
is clean.

## Low and informational

| Finding                                                                                              | Status                                                           |
| ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| `cors.allowed-origins` can be configured as `*` while `allowCredentials(true)`                       | open — validate at startup                                       |
| Swagger UI and `/v3/api-docs` are public in production                                               | open — disable or protect outside development                    |
| `GET /users` returns the JPA entity, including `jwtId` and the base64 picture                        | open — return a DTO                                              |
| `deleteUser` deletes the local row before Auth0, so an Auth0 failure leaves the account half-deleted | open                                                             |
| No HSTS/CSP/Referrer-Policy headers on the API or the Vercel app                                     | open — Caddy/Vercel config                                       |
| Backend publishes `8080:8080` on all interfaces; only Caddy should reach it                          | open — bind `127.0.0.1` or use an internal network               |
| No rate limiting on `/mcp` (token brute force is impractical, DoS is not)                            | open                                                             |
| Auth0 Management API token was cached in non-volatile fields                                         | fixed — `volatile` + `synchronized`                              |
| Auth0 DCR, once enabled, is open registration                                                        | accepted — strict mode, default permissions, monitor/rotate      |
| MCP protocol is 2025-06-18, not 2025-11-25/2026-07-28                                                | info — stateless transport is forward-compatible; upgrade branch |
| Historical `Co-Authored-By` trailers and the revoked Auth0 secret in history                         | info — not rewritten                                             |

## Not in scope

- Auth0 tenant configuration itself (MFA policy, anomaly detection, session
  lifetime, DCR hygiene). The setup checklist is in
  [`mcp-auth0-setup.md`](mcp-auth0-setup.md).
- Infrastructure hardening of the Hetzner host (firewall, fail2ban, Caddy TLS
  policy, database backups).
- Frontend dependency review (`npm audit`) — no dependency changes were made in
  this branch.
