# Security audit — 15 November 2026

Audit of `aurum-be` (Spring Boot API and MCP server) and `aurum-fe` (React SPA),
taken on `main` at `fcb94d3`; batch 1 landed in PR #95 (`f8c1c1e`) and batch 2 in
PR #117 on `chore/65-harden-deploy-and-report`. Read-only: no shard changed a tracked file,
started the application, or opened a database. Everything below comes from the
source, the built artefacts and the repository settings.

An issue number in a status column is the one that closes that finding; "Open
issues" says what each issue covers.

## Status key

| Status                   | Meaning                                                      |
| ------------------------ | ------------------------------------------------------------ |
| `fixed in #95 (f8c1c1e)` | landed in PR #95, merged as `f8c1c1e`                        |
| `fixed in this batch`    | landed in PR #117 (batch 2: build, deploy, CI, hosting, ops) |
| `open #<n>`              | accepted as work, tracked by an issue here                   |
| `accepted/info`          | accepted risk, by design, or context with no action          |

## Scope and method

Six read-only shards, each with its evidence file in
`.opencode/tasks/aurum-65/audit/` — a gitignored agent workspace, so not in the tree.
The names below say what backed each claim, not where to open it from a clone.

| Shard                         | Area                                                                        | Method                                                                                                            |
| ----------------------------- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `01-authz-idor.md`            | Authentication, authorization, IDOR, tenant isolation, privilege escalation | All 18 operations traced controller → facade → service → repository; every MCP tool traced to its ownership rule. |
| `02-input-validation.md`      | DTOs, mass assignment, type confusion, uploads, resource exhaustion         | Every request DTO and controller read; one probe on the app's own JDK to reproduce the image failure.             |
| `03-persistence-injection.md` | Injection, persistence integrity, query-level access control                | All 6 `@Query` and every derived finder, 31 Liquibase changesets, all 33 `@Transactional` boundaries.             |
| `04-web-xss-leaks.md`         | XSS, CSRF/CORS, headers, token storage, log and error leaks, secrets        | Whole-tree grep for every HTML sink and storage write; 172 `log.*` calls filtered; the built bundle inspected.    |
| `05-mcp-prompt-injection.md`  | MCP surface, prompt injection, LLM-specific threats                         | 24 tools traced into facades; library behaviour read from the resolved jars with `javap`.                         |
| `06-deps-infra-ci.md`         | Dependencies, container and infra configuration, CI/CD, deployment          | `dependency:tree`, `npm audit`, `gh api` repository settings, image inspection, `unzip -l` of the built jar.      |

### Tested and not found

Two whole classes were tested and are absent — the reason the rest of the list is
hardening rather than a live breach.

- **Cross-tenant access and IDOR.** Identity comes from the validated token principal,
  never from a body, path, query or header; no endpoint accepts a `userId`; every
  resource id is ownership-checked downstream; no role tier exists to escalate into.
  The per-endpoint table of what enforces each call is `01-authz-idor.md` §1.3; the
  repository methods reachable without a `userId` predicate are enumerated in
  `03-persistence-injection.md`.
- **SQL injection.** All JPQL is static with named parameters, every derived finder is
  a parse-time construct, and no `EntityManager`, `JdbcTemplate`, `CriteriaBuilder`,
  `Sort`, `Pageable` or `LIKE` exists in `src/main/java`. Full argument:
  `03-persistence-injection.md`, section "Is SQL injection currently possible?". The
  answer there is **no**.

Also clean, so not re-opened without new evidence: no HTML sink in the SPA, no open
redirect, no secret in the tracked tree or the production bundle, no PII in logs, no
entity bound from JSON, no upload filename used as a path.

## Corrections to the November 2026 audit

The previous audit was treated as claims and re-verified item by item. Most held;
two did not.

### 1. `deleteUser` does not half-delete when Auth0 fails

The November record said the method "deletes the local row before Auth0, so an Auth0
failure leaves the account half-deleted". It does not: `deleteUser` is
`@Transactional` and rethrows the `Auth0Exception` as a `RuntimeException`, so the
pending delete is never flushed and the local account survives intact. Evidence:
`03-persistence-injection.md`, the `deleteUser` finding; `01-authz-idor.md` §5, last
rows.

The real defect is the opposite asymmetry — **deletion is not durable**. The access
token keeps validating and the next request re-creates the user row through
`findOrCreate`, so a deleted account silently returns as an empty one. GDPR closure
needs a tombstone or an `app_metadata` flag checked before creation, plus session
revocation (#140), which also covers the ghost-account mirror risk.

### 2. HSTS at Caddy protects the API host only, not the SPA

The November Low table said "HSTS/Referrer-Policy added at Caddy" and read as
coverage for the application. `docs/operations.md` puts Caddy in front of
`api.aurum-networth.com` alone; `aurum-networth.com` is served by Vercel, from a
`vercel.json` whose only header was a `Content-Type` for the web manifest. The origin
holding the Auth0 refresh token had no CSP, HSTS, frame options or referrer policy.
Evidence: the SPA-header findings in `04-web-xss-leaks.md` and `06-deps-infra-ci.md`.

Headers now ship from `vercel.json`; the policy itself — CSP with `connect-src`,
`frame-ancestors`, a nonce or hash for the inline theme script — is #137. Caddy is
unchanged and still covers the API host alone.

## Intentional behaviour changes in #95

Deliberate, contract-visible; clients and tests moved with them.

- `GET /analytics/projections?years=<1` answered `{}`; now `400 Years must be between 1
and 50`, checked in the facade so the MCP surface inherits it.
- Malformed, missing and non-coercible bodies answered `500 An unexpected error
occurred`; now `400` in the pinned envelope with the field attached, `413` when
  oversize, `415`/`405` for media-type and method mistakes.
- `GET /users` no longer serialises `jwtId`, `assets` or `categories`; the committed
  contract dropped the entity schemas nothing read.
- A default or foreign category keeps its sentence and status but now throws
  `AccessDeniedException`/`ConflictException`; `IllegalArgumentException` and
  `ResponseStatusException` no longer publish their message.
- A null owner id in the three ownership mappers throws instead of skipping the check —
  unreachable from HTTP and MCP, so the change is invisible.
- MCP list tools return `{items, totalCount, truncated}` instead of a bare array and
  tool errors are curated; a picture that fails processing answers `400` field `file`.

## Findings

Severity is the shard's own rating. Counts: **High 8, Medium 24, Low 34, Info 4** — 70
findings, none Critical.

### Shard 01 — authorization, IDOR, tenant isolation

| Id        | Finding                                                              | Severity | Status                        |
| --------- | -------------------------------------------------------------------- | -------- | ----------------------------- |
| AUD-01-1  | Refresh token and access token persisted in `localStorage`           | Medium   | accepted                      |
| AUD-01-2  | Account deletion not durable: the next request recreates the user    | Low      | open #140                     |
| AUD-01-3  | 403-vs-404 existence oracle on every foreign resource id             | Low      | accepted, not tracked         |
| AUD-01-4  | Unbounded snapshot ids on `DELETE /snapshots/bulk`                   | Low      | open #139                     |
| AUD-01-5  | Malformed path variable or unreadable body answered 500 with a trace | Low      | fixed in #95 (f8c1c1e)        |
| AUD-01-6  | Ownership helpers that silently widened when `userId` was null       | Low      | fixed in #95 (f8c1c1e) + #140 |
| AUD-01-7  | `GET /users` serialised the JPA entity (`jwtId`, base64 picture)     | Low      | fixed in #95 (f8c1c1e)        |
| AUD-01-8  | Swagger UI and `/v3/api-docs` public in production                   | Low      | fixed in this batch           |
| AUD-01-9  | MCP read tools are scope-free when scopes are unconfigured           | Info     | open #138                     |
| AUD-01-10 | MCP audience boundary rested on an exact path match                  | Info     | open #138                     |
| AUD-01-11 | First-request user creation could answer 401 under a race            | Info     | accepted/info (suspected)     |

### Shard 02 — input validation, DTOs, uploads, resource exhaustion

| Id        | Finding                                                                   | Severity | Status                        |
| --------- | ------------------------------------------------------------------------- | -------- | ----------------------------- |
| AUD-02-1  | Picture upload threw `ImagingOpException` for EXIF orientation 2–8        | High     | fixed in #95 (f8c1c1e)        |
| AUD-02-2  | `GET /analytics/projections?years=` unbounded: single-request heap growth | High     | fixed in #95 (f8c1c1e)        |
| AUD-02-3  | Every non-coercible JSON value answered 500, not a field-level 400        | High     | fixed in #95 (f8c1c1e)        |
| AUD-02-4  | Path/query conversion, missing parts, oversize uploads answered 500       | Medium   | fixed in #95 (f8c1c1e)        |
| AUD-02-5  | Cash-flow updates accepted any month, any sign, any magnitude             | Medium   | fixed in #95 (f8c1c1e)        |
| AUD-02-6  | Negative `initialValue` on asset creation bypassed the snapshot rule      | Medium   | fixed in #95 (f8c1c1e)        |
| AUD-02-7  | Client-supplied exchange rate trusted without bounds                      | Medium   | fixed in #95 (f8c1c1e)        |
| AUD-02-8  | Currency and locale setters accepted `null` and any ISO/BCP-47 value      | Medium   | fixed in #95 (f8c1c1e)        |
| AUD-02-9  | Unbounded REST bodies; the only bulk REST endpoint had no batch cap       | Medium   | open #139                     |
| AUD-02-10 | 40 megapixels per upload, no heap ceiling and no concurrency budget       | Low      | accepted                      |
| AUD-02-11 | Numeric overflow answered 500; scale was silently rounded                 | Low      | fixed in #95 (f8c1c1e)        |
| AUD-02-12 | `currentAmount` on targets accepted unvalidated                           | Low      | fixed in #95 (f8c1c1e) + #139 |
| AUD-02-13 | MCP identifiers came from the body, unvalidated                           | Low      | fixed in #95 (f8c1c1e) + #138 |
| AUD-02-14 | Unbounded reference dates for manual assets                               | Low      | open #139                     |
| AUD-02-15 | Log injection through user-controlled name and filename                   | Low      | fixed in #95 (f8c1c1e)        |

### Shard 03 — injection, persistence integrity, query-level access control

| Id        | Finding                                                                     | Severity | Status                            |
| --------- | --------------------------------------------------------------------------- | -------- | --------------------------------- |
| AUD-03-01 | Projections raised an unbounded `years` to a BigDecimal power               | High     | fixed in #95 (f8c1c1e)            |
| AUD-03-02 | Bulk writes not atomic: a failing item left earlier ones committed          | Medium   | open #140                         |
| AUD-03-03 | `User.locale` persisted as a JVM ordinal while every other enum is `STRING` | Medium   | open #140                         |
| AUD-03-04 | `Asset` had no `@Version`, so an edit racing the scheduler was lost         | Low      | fixed in #95 (f8c1c1e) + #140     |
| AUD-03-05 | `deleteUser` made a network call inside its transaction                     | Low      | open #140 (description corrected) |
| AUD-03-06 | Cash-flow `year`/`month` unvalidated; the DB constraint produced a 500      | Low      | fixed in #95 (f8c1c1e)            |
| AUD-03-07 | Snapshot `referenceDate` unbounded, which drove the exchange-rate cache     | Low      | open #139                         |

### Shard 04 — XSS, CORS, headers, token storage, leaks, secrets

| Id        | Finding                                                                     | Severity | Status                            |
| --------- | --------------------------------------------------------------------------- | -------- | --------------------------------- |
| AUD-04-1  | Access and refresh tokens persist in `localStorage` — the XSS blast radius  | High     | accepted                          |
| AUD-04-2  | The SPA origin shipped no security headers at all                           | High     | fixed in this batch; CSP accepted |
| AUD-04-3  | CORS accepted `*` with `allowCredentials(true)`, unvalidated at startup     | Medium   | fixed in #95 (f8c1c1e)            |
| AUD-04-4  | Swagger UI and `/v3/api-docs` public in production                          | Medium   | fixed in this batch               |
| AUD-04-5  | `GET /users` returned the entity, two collections and the base64 picture    | Medium   | fixed in #95 (f8c1c1e)            |
| AUD-04-6  | Two error handlers echoed the exception's own message to the client         | Low      | fixed in #95 (f8c1c1e)            |
| AUD-04-7  | The Auth0 `sub` was logged at `INFO` in nine places                         | Low      | fixed in #95 (f8c1c1e)            |
| AUD-04-8  | User-controlled strings reached the logs verbatim (log injection)           | Low      | fixed in #95 (f8c1c1e)            |
| AUD-04-9  | Unhandled exceptions logged with SQL and row values in the cause chain      | Low      | accepted                          |
| AUD-04-10 | Frontend build fell back to the production Auth0 tenant when `VITE_*` unset | Info     | accepted                          |

### Shard 05 — MCP server, prompt injection, LLM-specific threats

| Id        | Finding                                                                         | Severity | Status                     |
| --------- | ------------------------------------------------------------------------------- | -------- | -------------------------- |
| AUD-05-01 | User names reached model context unmarked and unfiltered (stored injection)     | Medium   | fixed in #95, rest in #138 |
| AUD-05-02 | No confirmation boundary for writes; tools advertised no destructive hint       | Medium   | open #138                  |
| AUD-05-03 | Read tools returned unbounded collections: context flooding, cost amplification | Medium   | fixed in #95 (f8c1c1e)     |
| AUD-05-04 | Tool errors returned verbatim: existence oracle and schema disclosure           | Low      | fixed in #95 (f8c1c1e)     |
| AUD-05-05 | Origin validation was an exact match on the raw URI and could be skipped        | Low      | open #138                  |
| AUD-05-06 | Nothing prevented `mcp.audience` from equalling the web audience                | Low      | open #138                  |
| AUD-05-07 | Reads failed open when scopes were unset, and nothing forced them to be set     | Low      | open #138                  |
| AUD-05-08 | No body limit and no rate limit on `/mcp`; the batch cap ran after parsing      | Low      | open #139                  |

### Shard 06 — dependencies, container, CI/CD, deployment

`npm audit` reports 7 moderate advisories, all one dev-only `minimatch` chain;
`--omit=dev` reports zero. No CVE claim is made about the Maven versions.

| Id        | Finding                                                                                                         | Severity | Status                                 |
| --------- | --------------------------------------------------------------------------------------------------------------- | -------- | -------------------------------------- |
| AUD-06-1  | Local `application-local.properties` baked into the jar: a real Auth0 Management credential inside the artefact | High     | fixed in this batch                    |
| AUD-06-2  | `workflow_dispatch` deployed any branch to production, bypassing protection                                     | High     | fixed in this batch                    |
| AUD-06-3  | No `.dockerignore`: the local PostgreSQL data directory shipped as context                                      | Medium   | fixed in this batch                    |
| AUD-06-4  | Deploy was neither verified nor reversible                                                                      | Medium   | fixed in this batch                    |
| AUD-06-5  | Actions pinned to mutable tags; no provenance, SBOM or signature                                                | Medium   | accepted                               |
| AUD-06-6  | SSH deploy without host-key verification, plus a long-lived PAT on the host                                     | Medium   | accepted                               |
| AUD-06-7  | The frontend had no security headers, and Caddy does not cover that origin                                      | Medium   | fixed in this batch; CSP accepted      |
| AUD-06-8  | The application connects to PostgreSQL as the superuser                                                         | Medium   | accepted                               |
| AUD-06-9  | Branch protection requires neither E2E nor a pull request                                                       | Medium   | accepted                               |
| AUD-06-10 | Secret scanning, push protection and Dependabot updates disabled on a public repo                               | Medium   | accepted                               |
| AUD-06-11 | No automated dependency alerting or audit step in CI                                                            | Low      | fixed in this batch; settings accepted |
| AUD-06-12 | Backup is one copy on the failing host, unencrypted, with no restore test                                       | Medium   | accepted                               |
| AUD-06-13 | Runtime image is a full JDK on a floating tag, with `apt` and no healthcheck                                    | Low      | accepted                               |
| AUD-06-14 | `spring-boot-docker-compose` shipped on the runtime classpath                                                   | Low      | fixed in this batch                    |
| AUD-06-15 | No resource limits and no log rotation on either container                                                      | Low      | accepted                               |
| AUD-06-16 | One flat network: the backend and the database have no boundary                                                 | Low      | accepted                               |
| AUD-06-17 | `swagger-ui` webjar still ships; its endpoints are no longer public                                             | Low      | fixed in this batch                    |
| AUD-06-18 | gitleaks allowlist still covered a source file; the local hook silently skipped                                 | Low      | accepted                               |
| AUD-06-19 | `sed -i`/append of deploy variables into `.env` is newline-injectable                                           | Low      | accepted                               |

The first and third are why the batch existed: the guard was a `.gitignore`, which
does not apply to `docker build`. CI now asserts the packaged jar holds no local
properties file and no Docker Compose module.

## Open issues

The status columns above are the reverse map. After the audit landed, the
backlog was cut to one task per area that still has work; two areas were
accepted by the owner instead of tracked (see below).

| Task | Title                                                                  | What it closes                                                                                   |
| ---- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| #138 | `chore(mcp)`: close the MCP authorization and injection gaps           | AUD-01-9, AUD-01-10, AUD-02-13 remainder, AUD-05-01 remainder, AUD-05-02, AUD-05-05 to AUD-05-07 |
| #139 | `fix(api)`: bound the remaining request inputs                         | AUD-01-4, AUD-02-9, AUD-02-10, AUD-02-14, AUD-03-07, AUD-05-08                                   |
| #140 | `fix(api)`: make writes atomic and account deletion durable            | AUD-01-2, AUD-01-6 remainder, AUD-03-02, AUD-03-03, AUD-03-04, AUD-03-05                         |
| #142 | `chore(deps)`: upgrade the Auth0 Management SDK and metadata-extractor | the two stale dependencies called out by shard 06                                                |

Two issues pre-date the audit and are unrelated to it: #61 (backend coverage)
and #94 (duplicate snapshots). They stay as they are.

## Accepted and informational

| Item                                                              | Why it stands                                                                                                                                                                                                                                |
| ----------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CSRF disabled                                                     | All three chains are stateless and bearer-only; no cookie, no form login, no ambient credential to ride.                                                                                                                                     |
| No rate limiting on the API and `/mcp`                            | Brute force is impractical; the DoS half stays a body cap (#139) and an edge-proxy concern.                                                                                                                                                  |
| Auth0 DCR, once enabled, is open registration                     | Accepted with strict mode and default permissions; a tenant setting, not repository code.                                                                                                                                                    |
| CORS `allowedOriginPatterns` with credentials on the API          | Bearer-only, no cookies, so a reflected origin buys nothing without the token. A bare `*` now fails startup (#95).                                                                                                                           |
| MCP SDK 2.1 supports protocol 2025-11-25                          | Stateless transport is the shape that revision requires; the SDK cannot negotiate a newer one yet.                                                                                                                                           |
| Historical `Co-Authored-By` trailers and the revoked Auth0 secret | Deliberately not rewritten; forks and old clones still hold the value, and the secret was revoked.                                                                                                                                           |
| A locally built image may have carried the local properties file  | The mechanism inside the jar was proven; whether such an image was ever pushed is not answerable from the repo.                                                                                                                              |
| First-user-creation race                                          | Suspected, not reproduced. A bounded retry would close it; nothing else does today.                                                                                                                                                          |
| 403 vs 404 on a foreign id                                        | The mismatch exposes that an id exists, not its content; changing the semantics costs more than it returns. Recorded, not tracked.                                                                                                           |
| Tokens in `localStorage` and no CSP                               | Accepted by the owner on 15 November 2026. The app renders no raw HTML today, and the cost — a possible login after every reload — was judged higher than the reduced blast radius. Revisit if a dependency starts rendering untrusted HTML. |
| Host, pipeline and repository posture                             | Accepted by the owner on 15 November 2026: host-key verification, action pinning, the least-privilege database role, offsite backups, branch protection, secret scanning and Dependabot alerts stay as they are.                             |
| Reviewer nits from #95                                            | A weak Mockito matcher in `AssetServiceTest` and one synchronous assertion in a profile test. Fixed when those files are next touched, not as backlog.                                                                                       |

## Not in scope

- **Auth0 tenant configuration** — MFA, anomaly detection, DCR hygiene, API
  permissions, refresh-token rotation, token lifetime. Two findings depend on it and
  could only be verified from the client side: [`mcp-auth0-setup.md`](mcp-auth0-setup.md).
- **Host state** — the Caddyfile, the backup cron file, firewall and SSH
  configuration. None is in the repository, so the HSTS and rate-limit claims could not
  be checked.
- **CVE status of the pinned Maven versions** — no scanner or advisory database
  offline; versions are reported as resolved, not as advisory-free.
- **MCP client rendering and runtime probing** — how a client renders tool text is
  outside this boundary, and illegal-request behaviour is derived from code paths.

## Operations

Deploy, rollback, backup and restore, logs, migrations and host invariants are not
repeated here; the authoritative document is [`operations.md`](operations.md).

## History

- **November 2026** — the first audit: the MCP authorization layer, the custom OAuth
  proxy it replaced, and the frontend findings as they stood. Superseded by this
  record; its open items were re-verified above and two of its claims corrected.
- **15 November 2026, after the fix batches** — the 24 follow-up issues opened during
  the fix were consolidated into the six tasks above. Twenty were duplicates of each
  other by area, two were reviewer nits, one was accepted, and none carried
  information the findings here did not already hold.
- **15 November 2026, backlog decision** — the owner accepted the token-storage/CSP
  risk and the host/repository posture, and kept #138, #139, #140 and #142 as the
  remaining work. #61 and #94 stay independent of this audit.
