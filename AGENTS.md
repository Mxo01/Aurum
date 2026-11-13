# AGENTS.md

Project instructions for AI coding agents. Shared baseline rules (git flow,
testing, architecture, safety) come from the user-level `AGENTS.md`; this file
adds project-specific detail and wins on conflict.

## Project Overview

Aurum is a full-stack personal wealth management app. Users track financial assets, view net worth over time, set financial targets, and get analytics — with multi-currency support and dark/light theming.

## Repository Structure

Monorepo with two apps:
- `aurum-be/` — Spring Boot 4 backend (Java 25)
- `aurum-fe/` — React 19 + Vite 8 frontend (TypeScript)
- `Makefile` — orchestrates both apps

## Development Commands

### Full Stack
```bash
make start    # Start frontend + backend + Docker PostgreSQL (uses local Spring profile for backend)
make stop     # Stop all services
make restart  # Restart all services
```

> `make start` passes `--spring.profiles.active=local` to the backend, which loads secrets from `aurum-be/src/main/resources/application-local.properties`. This file is gitignored and must be created locally with the required secret values (see Secret Variables below).

### Frontend (`aurum-fe/`)
```bash
npm start            # Vite dev server on port 4200
npm run build        # typecheck + production build
npm run lint         # ESLint
npm run typecheck    # tsc --noEmit
npm test             # Vitest watch
npm run test:run     # Vitest once
npm run test:coverage
npm run test:e2e     # Playwright + axe
npm run api:types    # regenerate types from aurum-be/openapi.json
```

### Backend (`aurum-be/`)
```bash
docker compose up         # Start PostgreSQL 17 on port 5432
./mvnw spring-boot:run    # Start Spring Boot on port 8080
./mvnw test               # Run tests
./mvnw clean package      # Build JAR
```

### Swagger UI
Available at `http://localhost:8080/swagger-ui.html` when backend is running.

## Architecture

### Backend Domain Structure
Each domain under `src/main/java/com/backend/aurum/domain/` follows:

```
controller/ → facade/ → service/ → repository/ → model/
```

- **asset/** — Core domain: assets, snapshots, categories, liability types
- **analytics/** — KPI calculation and financial target management
- **user/** — User profiles with currency/locale preferences
- **auth/** — Auth0 management API integration

#### Facade Layer
Each domain has a `facade/` package containing `*Facade` classes that are the **single entry point** for all business operations. Both HTTP controllers and `AurumMcpTools` must go through the facade — never call a domain service directly from a controller or MCP tool.

Each Facade owns: validation (via `*ValidationService`) → DTO-to-entity mapping (via `*Mapper`) → domain service call → entity-to-DTO mapping.

```
AssetController  ──┐
                   ├──▶  AssetFacade  ──▶  AssetService
AurumMcpTools  ────┘
```

Facades per domain:
- `asset/facade/` — `AssetFacade`, `SnapshotFacade`, `AssetCategoryFacade`
- `analytics/facade/` — `TargetFacade`, `AnalyticsFacade`

#### DTO conventions
- **Response DTO** (e.g. `AssetDTO`, `TargetDTO`) — returned by all endpoints and facade methods
- **Create DTO** (e.g. `CreateAssetDTO`) — used as request body for `POST` endpoints and MCP create tools
- **Update DTO** (e.g. `UpdateAssetDTO`) — used as request body for `PUT` endpoints and MCP update tools; always includes the entity `id`

Controllers accept Create/Update DTOs for write operations; the combined response DTO is never used as a write request body.

Infrastructure:
- `infrastructure/security/` — JWT/OAuth2 resource server config (stateless, Auth0)
- `infrastructure/mcp/` — protected-resource metadata, MCP audience/scopes, origin filter, tool scope guard
- `infrastructure/exchange/` — External exchange rate API service
- `infrastructure/config/` — OpenAPI/Swagger configuration

### Frontend Structure
Feature-sliced React app under `aurum-fe/src/`:
- `app/` — providers, router, shell (top bar, bottom tabs, command palette)
- `features/<domain>/` — page, components, and query hooks: `dashboard`, `assets`,
  `categories`, `snapshots`, `cashflow`, `targets`, `profile`, `legal`, `landing`
- `lib/` — typed API client and generated types, keys, formatting, stores, utils
- `components/ui/` — vendored shadcn primitives; `components/app/` — shared app components
- `test/` — MSW server, fixtures, render helper

Rules:
- A feature never imports from another feature. Shared code goes to `lib/` or
  `components/`.
- Pages do not call `fetch`: they use the feature's TanStack Query hooks, and
  hooks use the `useApi()` client built with `openapi-fetch`.
- Server state lives in TanStack Query only. Client state lives in Zustand
  (theme, privacy, command palette) or the URL (year, filters). No Redux.
- Query keys are centralized in `lib/api/keys.ts`; every mutation invalidates
  what it changes.
- UI: warm neutral + gold accent, Geist Sans/Mono. No glass, glow, dot-grid,
  serif italics, or uppercase tracking. Details open in sheets, not new pages.

### Data Model
```
User → AssetCategories (1:N)
User → Assets (1:N) → Snapshots (1:N)   # Snapshots are historical values
User → Targets (1:N)
Asset → AssetCategory (N:1)
```

`Snapshot` stores `amountOriginalCurrency` + `exchangeRateToBase` — use `getAmountInBaseCurrency()` for the converted value.

Assets have `LiabilityType` (MANUAL/AUTOMATIC) and `PaymentFrequency` (WEEKLY/MONTHLY/YEARLY). Automatic liabilities are processed by `LiabilitySchedulerService` (scheduled via `@EnableScheduling`).

### Authentication
- Frontend uses `@auth0/auth0-react` — Auth0 login redirects; the access token is attached by the `openapi-fetch` client
- Backend validates JWT via Spring Security OAuth2 resource server (`issuer-uri` + `audiences` in `application.properties`)
- The MCP endpoint validates a separate audience (`mcp.audience`, defaulting to `mcp.resource-url`); a web-app token is not valid there. Setup: `docs/mcp-auth0-setup.md`
- All endpoints require auth except Swagger paths and the protected-resource metadata

### Database
- PostgreSQL 17 via Docker Compose (`compose.yaml`) — same setup for local dev and production (Hetzner VPS)
- Migrations managed with **Liquibase** YAML files in `src/main/resources/db/changelog/changes/`
- Local connection: `localhost:5432/aurum`, user: `user`, password: `password`

## Key Conventions

- Backend uses **Lombok** extensively — `@Data`, `@Builder`, `@RequiredArgsConstructor` on models/DTOs
- Frontend is **React 19 + Vite 8 + TypeScript strict** (no `any`), **Tailwind CSS 4** and **shadcn/ui** (Radix). Prefer a shadcn primitive or a Radix package over a custom widget
- Data fetching is **openapi-fetch** + **TanStack Query**; forms are **react-hook-form + zod**; charts are **Recharts**
- Never hand-write a request/response shape: generate it from `aurum-be/openapi.json`
- Currency amounts use `BigDecimal` on the backend; format with user's locale/currency on the frontend
- Never add `: void` as an explicit return type on functions or methods — omit it entirely
- Git pre-commit hook runs ESLint + Prettier via lint-staged (husky) for the frontend, and `spotless:apply` for any staged Java files in the backend
- Backend Java formatting uses **Prettier + prettier-plugin-java** (same tool as frontend), reading `prettier.config.cjs` at the repo root — run `npx prettier --write 'aurum-be/src/**/*.java'` manually or let the pre-commit hook handle it

## Frontend Testing

### Setup
- Unit and integration: **Vitest** + **React Testing Library** + **MSW** +
  `@faker-js/faker`. Run with `npm test` / `npm run test:coverage`.
- End-to-end: **Playwright** + `@axe-core/playwright`. Run with `npm run test:e2e`.
- Coverage: `@vitest/coverage-v8`.

### Rules
- **100% coverage** of statements, branches, functions and lines is the gate.
  The only exclusions are generated or vendored files (`src/components/ui/**`,
  `src/lib/api/schema.d.ts`, `src/main.tsx`, `*.d.ts`). No exclusion for a
  component that is merely hard to test.
- Mock every external dependency: network, Auth0, clock, storage. A unit test
  never touches the real world.
- Assign the unit under test to `testSubject`.
- Build inputs with factories/builders and named fixtures; assert against named
  expectations. Table-driven tests over copy-pasted cases.
- Test the public surface. Query by role, label or text, not by test id, unless
  no accessible alternative exists.
- Query hooks are tested with `renderHook` + MSW, including the error path.
- No snapshot tests. No `any`. Comments only for `// GIVEN`, `// WHEN`, `// THEN`.
- E2E runs against the production build (`vite build` + `vite preview`) with a
  stateful in-memory API mock and a network-stubbed Auth0. Deterministic clock,
  no `waitForTimeout`, fresh context per test, axe scan on the main pages.
- A bug fix ships with a test that fails before the fix and passes after.

## Backend Testing

### Setup
- Test runner: **JUnit 5** via `spring-boot-starter-test` — run with `./mvnw test` (or `./mvnw test -Dtest="!AurumApplicationTests"` to skip the integration context test that requires a live DB)
- Mocking: **Mockito** (included via `spring-boot-starter-test`) — `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`
- Random test data: **Instancio** (`instancio-junit`) — `Instancio.create(MyClass.class)` for fully-populated random instances
- Mock maker: `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` is set to `mock-maker-subclass` (required for Java 25 compatibility with Spring Data repository interfaces)

### Rules

- **No comments** except `// GIVEN`, `// WHEN`, `// THEN` block separators
- **No `any`** — never use `any` for casting or accessing private members; never use `any()` as a Mockito matcher when a specific value or `eq()` can be used instead
- **No `verify()` unless void** — for non-void methods, assert the returned value; only use `verify()` for void method calls
- **No private method testing** — test behavior through the public API only
- **Test subject naming** — the class under test must always be assigned to a variable named `testSubject`
- **Mock everything** — all dependencies must be mocked; never rely on real implementations
- **Test behavior, not values** — use mock objects for test data; assert that the correct mock result flows through, not hardcoded field values
- **BDD structure** — `// GIVEN`, `// WHEN`, `// THEN` comments to separate test body sections; use descriptive `@Test` method names that read as sentences

### Annotations
```java
@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService testSubject;
}
```

### GIVEN naming
- Mock object instances: `mock<Name>` (e.g. `mockAsset`)
- Stub return values: `stubbed<Name>` (e.g. `stubbedAssets`)

### THEN naming
- Resolved/actual values: `expected<Name>` (e.g. `Asset expectedAsset = result`)

### Example — Service
```java
@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService testSubject;

    @Test
    void findById_returnsAsset_whenAuthorized() {
        // GIVEN
        UUID mockUserId = UUID.randomUUID();
        Asset mockAsset = Instancio.of(Asset.class)
            .set(Select.field(Asset::getUser), buildMockUser(mockUserId))
            .create();
        when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

        // WHEN
        Asset expectedAsset = testSubject.findById(mockAsset.getId(), mockUserId);

        // THEN
        assertThat(expectedAsset).isEqualTo(mockAsset);
    }

    @Test
    void delete_delegatesToRepository() {
        // GIVEN
        UUID mockUserId = UUID.randomUUID();
        Asset mockAsset = Instancio.of(Asset.class)
            .set(Select.field(Asset::getUser), buildMockUser(mockUserId))
            .create();
        when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

        // WHEN
        testSubject.delete(mockAsset.getId(), mockUserId);

        // THEN
        verify(assetRepository).delete(mockAsset);
    }
}
```

## Testing Requirements

**Always add tests for new code — this is implied by every code generation request.** Whenever you create or modify a feature, service, component, or fix a bug, you must also write the corresponding tests in the same response, without being asked. Every new feature, service method, component, or bug fix must be accompanied by tests:
- Backend: JUnit 5 unit tests following the rules in the [Backend Testing](#backend-testing) section
- Frontend: Vitest/RTL/MSW unit and integration tests plus Playwright e2e, following the [Frontend Testing](#frontend-testing) section. New frontend code must keep the 100% coverage gate green

PRs without tests for new code will not be merged.

## SEO Maintenance

Whenever frontend routes are added, removed, or renamed, you must update both:
- `aurum-fe/public/robots.txt` — adjust `Disallow`/`Allow` entries if the new route should be crawled or blocked
- `aurum-fe/public/sitemap.xml` — add, remove, or update the corresponding `<url>` entry (including `<loc>`, `<changefreq>`, and `<priority>`)

This applies to any route change, including lazy-loaded routes defined in `app.routes.ts` or any child route file.

## Branching Rules

**Never commit directly to `main`.** When starting any edit, check the current branch:

- If on `main` → create a new branch before making any changes.
- If already on a feature/fix branch (branched off `main`) → continue on that branch; do **not** create another branch even if the task or context changes.

Branch naming:
- Feature or enhancement → `feat/<short-name>`
- Bug fix → `fix/<short-name>`

Use a concise kebab-case short name that describes the change (e.g., `feat/target-progress-bar`, `fix/snapshot-currency-display`).

## Hosting

- **Frontend** — deployed on **Vercel** (connected to the GitHub repo, auto-deploys on push to `main`) at `aurum-networth.com`
- **Backend + Database** — deployed on a **Hetzner VPS** via Docker Compose (same `compose.yaml` used locally, with production environment variables injected on the server) at `api.aurum-networth.com`

### Domain / Routing Structure

- `aurum-networth.com` → Vercel (React SPA)
- `api.aurum-networth.com` → Hetzner VPS → Spring Boot on port 8080

Backend endpoints are served **directly at the root** of `api.aurum-networth.com` (e.g. `api.aurum-networth.com/users`, `/assets`). There is **no `/api` path prefix** — the subdomain itself conveys that. Any reverse proxy (nginx/Caddy) on the VPS should forward all traffic straight to port 8080 without adding a path prefix.

## Secret Variables

Secrets are never committed. They are managed via environment-specific config:

### Backend
- Local: `aurum-be/src/main/resources/application-local.properties` (gitignored) — loaded when `spring.profiles.active=local` (i.e., via `make start`)
- Production: set as environment variables on the Hetzner VPS

Key secrets required:
```
# Auth0
spring.security.oauth2.resourceserver.jwt.issuer-uri=
auth0.management.domain=
auth0.management.client-id=
auth0.management.client-secret=
auth0.management.audience=

# Exchange rate API
exchange.api.key=

# Database (production Hetzner VPS — local uses Docker defaults)
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
```

### Frontend
There are no frontend secrets. Vite reads public build-time configuration from
`VITE_*` environment variables (local `.env.local`, production Vercel env vars):

```
VITE_API_URL=
VITE_AUTH0_DOMAIN=
VITE_AUTH0_CLIENT_ID=
VITE_AUTH0_AUDIENCE=
```

The Auth0 SPA client ID, domain and audience are public by design: they ship in
the browser bundle. Never put a client secret in the frontend.

## Audit

When asked to perform an audit (`esegui un audit`), always perform a **comprehensive codebase audit** covering all of the following areas. If anything else comes to mind during the audit, include it — the software must be solid and optimized.

### Audit Checklist

1. **Potential bugs** — null handling, edge cases, race conditions, incorrect business logic
2. **UI responsiveness** — fixed widths, missing responsive breakpoints, overflow issues, mobile-unfriendly layouts
3. **UX problems** — missing loading states, empty states, error feedback, form validation gaps, navigation issues, accessibility (a11y)
4. **Security vulnerabilities** — XSS, SQL injection, CSRF, input validation, CORS misconfiguration, secrets exposure, authentication/authorization gaps
5. **GDPR & privacy** — data export completeness (Art. 20), data retention policies (Art. 5), cookie disclosure, user deletion cascade, PII in logs
6. **Data leaks** — tokens/secrets in logs or responses, sensitive data in localStorage, unencrypted PII, error messages exposing internals
7. **Optimization & efficiency** — unnecessary re-renders, missing caching, redundant computations, large bundle imports, lazy loading gaps
8. **Database indexing** — missing indexes on foreign keys, frequently filtered columns, composite indexes for common query patterns
9. **Query optimization** — N+1 queries, redundant DB calls, missing `@Transactional(readOnly = true)`, eager vs lazy loading issues, batch operations
10. **Service/repository interaction** — redundant calls across facade → service → repository layers, missing caching layers, batch operations
11. **First user access flow** — race conditions on user creation, default values, onboarding data seeding, transaction isolation
12. **SEO** — robots.txt, sitemap.xml, meta tags, structured data, SSR/pre-rendering for public pages
13. **Test quality & coverage** — missing test files, missing edge cases, incorrect mocking patterns, adherence to project test conventions
14. **Memory leaks** — event listeners, timers, observers, subscriptions or abort controllers not cleaned up on unmount; state updates after unmount

### Audit Tools

Use **Playwright MCP server** and **frontend-design plugin** to visually verify UI quality, responsiveness, and UX during the audit when applicable.

### Audit Output

Present findings in a prioritized table format (CRITICAL → HIGH → MEDIUM → LOW) with:
- Issue identifier
- File path and line number
- Clear description of the problem
- Suggested fix
