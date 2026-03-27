# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Aurum is a full-stack personal wealth management app. Users track financial assets, view net worth over time, set financial targets, and get analytics — with multi-currency support and dark/light theming.

## Repository Structure

Monorepo with two apps:
- `aurum-be/` — Spring Boot 4 backend (Java 25)
- `aurum-fe/` — Angular 21 frontend (TypeScript)
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
npm start           # ng serve with HMR on port 4200
npm run build       # Production build
npm run lint        # ESLint fix
npm run format      # Prettier format
npm run clean-code  # lint + format combined
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
- `infrastructure/exchange/` — External exchange rate API service
- `infrastructure/config/` — OpenAPI/Swagger configuration

### Frontend Domain Structure
Lazy-loaded Angular routes under `src/app/domain/`:
- `asset/` — Asset CRUD with form, table, category, and history components
- `dashboard/` — Main overview with Chart.js visualizations
- `snapshot/` — Point-in-time asset value management
- `target/` — Financial goals tracking
- `profile/` — User preferences (currency, locale)

Shared: `src/app/shared/services/` contains `NavigationService` and `ThemeService`.

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
- Frontend uses `@auth0/auth0-angular` — Auth0 login redirects, JWT attached via HTTP interceptor
- Backend validates JWT via Spring Security OAuth2 resource server (`issuer-uri` + `audiences` in `application.properties`)
- All endpoints require auth except Swagger paths

### Database
- PostgreSQL 17 via Docker Compose (`compose.yaml`) — same setup for local dev and production (Hetzner VPS)
- Migrations managed with **Liquibase** YAML files in `src/main/resources/db/changelog/changes/`
- Local connection: `localhost:5432/aurum`, user: `user`, password: `password`

## Key Conventions

- Backend uses **Lombok** extensively — `@Data`, `@Builder`, `@RequiredArgsConstructor` on models/DTOs
- Frontend uses **standalone Angular components** (no NgModules)
- Frontend UI is **PrimeNG 21** + **Tailwind CSS 4** — **always prefer PrimeNG components over custom HTML/CSS implementations**; only build custom components when PrimeNG has no equivalent
- Currency amounts use `BigDecimal` on the backend; format with user's locale/currency on the frontend
- Git pre-commit hook runs ESLint + Prettier via lint-staged (husky) for the frontend, and `spotless:apply` for any staged Java files in the backend
- Backend Java formatting uses **Prettier + prettier-plugin-java** (same tool as frontend), reading `prettier.config.cjs` at the repo root — run `npx prettier --write 'aurum-be/src/**/*.java'` manually or let the pre-commit hook handle it

## Frontend Testing

### Setup
- Test runner: **Vitest** via `@angular/build:unit-test` — run with `npm test` (or `npm run test:watch`)
- Coverage: **@vitest/coverage-v8** — run with `npm test -- --coverage`
- Mocking Angular schematics (services, components, directives, pipes): **ng-mocks** (`MockProvider`, `MockComponent`, etc.)
- Random test data generation: **@faker-js/faker**

### Rules

- **No comments** except `// GIVEN`, `// WHEN`, `// THEN` block separators
- **No `any`** — never use `any` for casting or accessing members
- **No private method testing** — test behavior through public API only
- **Test subject naming** — the class under test must always be assigned to a variable named `testSubject`
- **Mock everything** — all dependencies must be mocked; never rely on real implementations
- **No `MockBuilder`** — use `TestBed.configureTestingModule` with `MockProvider`, `MockComponent`, `MockDirective`, `MockModule` instead
- **No `protected` / no `*Internals`** — never use `protected` on component fields/methods accessed in tests; never cast with `as unknown as XxxInternals` to reach members; access all members directly via `testSubject`
- **Single `beforeEach`** — one `beforeEach` per describe block containing all `configureTestingModule`, `TestBed.inject`, and fixture/testSubject assignments
- **`TestBed.inject` once in `beforeEach`** — inject services into describe-scoped variables inside `beforeEach`; do not inject inside individual `it` blocks (exception: when a fresh instance is needed per test, e.g. constructor-time state)
- **Component isolation via mocked imports** — never use `NO_ERRORS_SCHEMA` and never use `TestBed.overrideComponent`; instead, mirror the component's own `imports` array in `configureTestingModule` by replacing every entry except the component under test with the matching ng-mocks wrapper:
  - Angular/PrimeNG/custom **components** → `MockComponent(Foo)`
  - Angular/PrimeNG/custom **directives** (including `RouterLink`, `RouterLinkActive`, `RouterOutlet`) → `MockDirective(Foo)`
  - **Modules** (`FormsModule`, `ReactiveFormsModule`, `TableModule`, etc.) → `MockModule(Foo)`
  - **Pipes** → `MockPipe(Foo)`
  - Because mocked sub-components carry no real DI tree, transitive service dependencies are never needed in `providers`
  - **Exception — aliased signal inputs**: if a third-party directive uses `input(null, { alias: 'foo' })` (e.g. `Highlight` from `ngx-highlightjs`), `MockDirective` cannot replicate the alias and Angular will throw `NG0303`. In this case, declare a minimal inline stub that matches the directive's **exact** selector and declares `@Input()` for every binding used in the template:
    ```typescript
    @Directive({ selector: '[highlight]', standalone: true })
    class HighlightStub {
      @Input() highlight!: string | null;
      @Input() language!: string;
    }
    ```
- **GIVEN naming** — mock object instances: `mock<Name>` (e.g. `mockAsset`); stub return values: `stubbed<Name>` (e.g. `stubbedAssets`)
- **THEN naming** — resolved/actual values: `expected<Name>` (e.g. `const expectedAssets = await result`)
- **HTTP success AND error** — for every HTTP-backed method write both a success case and an error case; in success tests assert the result: `expect(expectedResult).toEqual(stubbedResult)`
- **HTTP calls** — use `lastValueFrom()` with `async/await` and `HttpTestingController`; always call `httpController.verify()` in `afterEach`; match requests by URL suffix (`req.url.endsWith(...)`) or `req.urlWithParams` for query params
- **No `vi.mock`** — the Angular unit-test runner does not support `vi.mock` for relative imports; use TestBed providers for all mocking
- **Form controls** — access form controls via dot notation: `form.controls.name`, `form.controls.email`, etc. — never use bracket notation `form.controls["name"]`; also never use `form.get("")?.value`
- **`fixture.detectChanges()` over `TestBed.tick()`** — always use `fixture.detectChanges()` to flush effects and trigger change detection; never use `TestBed.tick()`; never combine the two in the same test
- **Max 1 nesting level** — at most one level of nested `describe` inside the outer `describe` of the class; do not add a second layer of `describe` blocks
- **BDD structure** — use `describe` blocks for grouping and `it` descriptions that read as sentences from the subject's perspective

### Example — Service

```typescript
vi.mock("../../../environments/environment", () => ({
  environment: { apiUrl: "http://test-api" }
}));

describe("MyService", () => {
  let testSubject: MyService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MyService, MockProvider(SomeDep), provideHttpClient(), provideHttpClientTesting()]
    });
    testSubject = TestBed.inject(MyService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpController.verify());

  describe("getItems", () => {
    it("should return items on success", async () => {
      // GIVEN
      const stubbedItems = [buildMockItem()];

      // WHEN
      const result = lastValueFrom(testSubject.getItems());
      httpController.expectOne(req => req.method === "GET" && req.url.endsWith("/items")).flush(stubbedItems);

      // THEN
      const expectedItems = await result;
      expect(expectedItems).toEqual(stubbedItems);
    });

    it("should throw on error", async () => {
      // WHEN
      const result = lastValueFrom(testSubject.getItems());
      httpController.expectOne(req => req.method === "GET").flush(null, { status: 500, statusText: "Server Error" });

      // THEN
      await expect(result).rejects.toThrow();
    });
  });
});
```

### Example — Component

Suppose `MyComponent` is declared as:

```typescript
@Component({
  standalone: true,
  imports: [SomeChildComponent, RouterLink, FormsModule, SomePipe]
  // ...
})
export class MyComponent {
  private readonly myService = inject(MyService);
  // ...
}
```

The spec mirrors the `imports` array, replacing every entry (except `MyComponent` itself) with its ng-mocks wrapper:

```typescript
describe("MyComponent", () => {
  let fixture: ComponentFixture<MyComponent>;
  let testSubject: MyComponent;
  let mockMyService: MyService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        MyComponent,
        MockComponent(SomeChildComponent),
        MockDirective(RouterLink),
        MockModule(FormsModule),
        MockPipe(SomePipe)
      ],
      providers: [MockProvider(MyService, { getData: vi.fn().mockReturnValue(of([])) })]
    });
    fixture = TestBed.createComponent(MyComponent);
    testSubject = fixture.componentInstance;
    mockMyService = TestBed.inject(MyService);
    fixture.detectChanges();
  });

  describe("someMethod", () => {
    it("should update state on success", () => {
      // GIVEN
      const stubbedData = [buildMockItem()];
      vi.spyOn(mockMyService, "getData").mockReturnValue(of(stubbedData));

      // WHEN
      testSubject.someMethod();
      fixture.detectChanges();

      // THEN
      expect(testSubject.items()).toHaveLength(stubbedData.length);
    });
  });
});
```

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

**Always add tests for new code.** Every new feature, service method, component, or bug fix must be accompanied by tests:
- Backend: JUnit 5 unit tests following the rules in the [Backend Testing](#backend-testing) section
- Frontend: Vitest unit tests following the rules in the [Frontend Testing](#frontend-testing) section

PRs without tests for new code will not be merged.

## Branching Rules

**Never commit directly to `main`.** When starting any edit and the current branch is `main`, always create a new branch first:

- Feature or enhancement → `feat/<short-name>`
- Bug fix → `fix/<short-name>`

Use a concise kebab-case short name that describes the change (e.g., `feat/target-progress-bar`, `fix/snapshot-currency-display`).

## Hosting

- **Frontend** — deployed on **Vercel** (connected to the GitHub repo, auto-deploys on push to `main`) at `aurum-networth.com`
- **Backend + Database** — deployed on a **Hetzner VPS** via Docker Compose (same `compose.yaml` used locally, with production environment variables injected on the server) at `api.aurum-networth.com`

### Domain / Routing Structure

- `aurum-networth.com` → Vercel (Angular SPA)
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
- Local: `aurum-fe/src/environments/environment.ts` (gitignored for sensitive values)
- Production: set as environment variables on Vercel

Key secrets required:
```
AUTH0_DOMAIN=
AUTH0_CLIENT_ID=
AUTH0_AUDIENCE=
API_BASE_URL=
```
