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
make start    # Start frontend + backend + Docker PostgreSQL
make stop     # Stop all services
make restart  # Restart all services
```

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
Each domain under `src/main/java/com/backend/aurum/domain/` follows: `controller/` → `service/` → `repository/` → `model/` pattern.

- **asset/** — Core domain: assets, snapshots, categories, liability types
- **analytics/** — KPI calculation and financial target management
- **user/** — User profiles with currency/locale preferences
- **auth/** — Auth0 management API integration

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
- PostgreSQL 17 via Docker (`compose.yaml`)
- Migrations managed with **Liquibase** YAML files in `src/main/resources/db/changelog/changes/`
- Connection: `localhost:5432/aurum`, user: `user`, password: `password`

## Key Conventions

- Backend uses **Lombok** extensively — `@Data`, `@Builder`, `@RequiredArgsConstructor` on models/DTOs
- Frontend uses **standalone Angular components** (no NgModules)
- Frontend UI is **PrimeNG 21** + **Tailwind CSS 4** — use PrimeNG components for consistency
- Currency amounts use `BigDecimal` on the backend; format with user's locale/currency on the frontend
- Git pre-commit hook runs ESLint + Prettier via lint-staged (husky)
