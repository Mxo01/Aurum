# Liabilities audit — November 2026 (`Aurum#59`)

The remediation is tracked in `Aurum#66`–`Aurum#70`. This document is the
evidence the fixes are built against.

Verdict: the liabilities flow is **not sound**. The scheduler runs, but its
arithmetic, its idempotency contract and its failure mode are all broken, and it
can lose a whole night's work while logging success. Analytics measures
liabilities correctly in the simple case and wrongly in nine others. No
cross-tenant leak was found.

Evidence: every CRITICAL and every HIGH below was reproduced against the real app
(Spring Boot 4.1, `local` profile) and real Postgres 17, on a throwaway database.
Line references come from the code as read. The reproduction commands and raw
logs are not published; each finding row carries the observations that decide it.

## Production state (read-only over SSH, every query in `BEGIN READ ONLY`)

- `databasechangelog` exists, 23 rows, last executed 2026-04-10 05:38, latest file
  `db/changelog/changes/2026-04-09-drop-api-keys.sql` — the newest file in the repo. Production's
  schema is **current**, not drifted.
- **Zero `AUTOMATIC` liabilities.** `assets` is exactly two shapes: 12 rows `liability_type=MANUAL,
  payment_frequency=MONTHLY, payment_amount=0.0000, original_currency=EUR`, and 3 rows with all
  three liability columns null (plain assets, EUR). The scheduler has never had a single row to
  process in production.
- Those 12 liabilities have 2–25 snapshots each; latest `reference_date` 2026-10-01 for nine,
  2026-01-01 for `Monitor LG 34" 2K`, `TV Hisense 44" 4K` and `Cash` (all three at amount 0), and
  2025-12-01 for `Paypal` (also 0).
- `docker logs aurum-be | grep -iE "liabilit|schedul|cron"` returns nothing.

**How to read the severity below.** No scheduler finding has production data at risk today: with
zero AUTOMATIC liabilities the job is *unexercised*, not *working*, and any claim that it works in
production is unsupported by data. All of C1–C3 and the scheduler HIGHs are latent in production
and live the moment a user turns the Automatic toggle on — the first such asset is the first real
run. The analytics findings (C4, H8–H11) are live today, on the 12 real liabilities.

## Findings — CRITICAL

| id | where | what is wrong | evidence | blast radius | fix (one line) |
|----|-------|---------------|----------|--------------|----------------|
| C1 | `LiabilitySchedulerService.java:35,47-68` | The whole batch is one `@Transactional`; the per-asset `try/catch` cannot see a failure raised at flush/commit, and the "completed" line is logged before the commit. | 3 due liabilities, one overflowing `numeric(19,4)`: log printed `3 processed, 0 failed out of 3 total`, then `DataIntegrityViolationException … at JpaTransactionManager.doCommit`; all 3 assets kept their old snapshot, the 2 healthy payments were lost. | Every user's payments for the night, silently, repeated every night until the offending row is fixed. | One transaction per asset (or per user), and log the summary after the commit. |
| C2 | `LiabilitySchedulerService.java:90-92,131-137` | Due-ness is anchored on the latest snapshot's date, not on a payment schedule: N missed periods produce exactly one payment and no backfill. | MONTHLY, balance 1000 on 2026-01-15, single run on 2026-04-15 → `amount=900`, not 700. Two payments vanished, no log of the gap. | Downtime, a failed run, a redeploy or a long C1 episode permanently under-charges every automatic liability. | Anchor on a persisted `nextDueDate`, and loop `while (due <= today)`. |
| C3 | `SnapshotService.java:104-105,112,145-160` + `SnapshotRepository.java:21` | `saveOrUpdate` overwrites *any* snapshot in the same month (unordered `findFirst…Between`) and rewrites its `referenceDate` to today. The scheduler and `POST /snapshots` share this path, so the cron eats the user's row. | WEEKLY liability, user snapshot `2026-03-10 amount 5000` (id `d04a12ae`); run on 2026-03-17 → the same id is now `2026-03-17 amount 4900`. The user's valuation is gone, no warning, no new row. | User-entered balances on automatic liabilities are silently destroyed, and the monthly history the chart depends on is rewritten. | Give automatic payments their own rows (never an upsert), and add a `source` column. |
| C4 | `AnalyticsService.java:52,323-328` (window `now−1y−2m`, no carry-in row) | Assets and liabilities whose last snapshot is older than ~14 months contribute 0 to the summary — and `totalLiabilities` is derived as `gross − net`, so a stale debt is invisible. | 15-month-old asset 100000 + liability 40000 → summary `gross 0 / liabilities 0 / net 0` while `/analytics/chart` for the same user returns `100000 / 60000`. Current asset 100000 + 15-month-old liability 40000 → `totalLiabilities=0.0000`, `totalNetWorth=100000.0000`. | Net worth overstated by the full amount of any stale liability; two endpoints of the same app report different numbers. On production data this fires on 2027-02-14 (`Paypal`, last snapshot 2025-12-01) and 2027-03-14 (three zero-value rows, last snapshot 2026-01-01). | Load one carry-in snapshot per asset before the window, or the latest snapshot per asset with no lower bound. |

## Findings — HIGH

| id | where | what is wrong | evidence | blast radius | fix (one line) |
|----|-------|---------------|----------|--------------|----------------|
| H15 | `aurum-be/pom.xml:68-69` (only `liquibase-core`) | `spring-boot-liquibase` is absent, so under Boot 4 `LiquibaseAutoConfiguration` never loads and no changeset runs; `spring.liquibase.*` is inert. Production is current only because no changeset has been added since it last ran. | Prod `databasechangelog`: 23 rows, last executed 2026-04-10 05:38, latest file `2026-04-09-drop-api-keys.sql` = the newest file in the repo. Classpath has `liquibase-core` and no `spring-boot-liquibase`; `git log -S spring-boot-liquibase -- aurum-be/pom.xml` is empty (Boot 3 shipped the autoconfiguration, Boot 4 moved it to its own module). Boot against an empty database fails with `Schema validation: missing table [asset_categories]` instead of creating it. | No damage today; the first new changeset added to the repo reaches no database, silently, because `ddl-auto=validate` only catches what Hibernate maps — not a new CHECK, UNIQUE or index. | Add `spring-boot-liquibase`, gate deploys on `liquibase:update`, and re-run it once to close the gap. |
| H1 | `LiabilitySchedulerService.java:50,63-68,72-101` | `processed++` runs whenever the method returns normally, so every skip is counted as a payment; skip branches log nothing. | 3 AUTOMATIC liabilities (null frequency, null amount, zero snapshots) → `3 processed, 0 failed out of 3 total`, zero rows written. A due + a skipped one → `2 processed` with one save line. | The only signal an operator has is wrong; a dead liability looks healthy forever. | Count `paid/skipped/failed` separately and log one line per skipped asset with a reason. |
| H2 | `AssetValidationService.java:11-36` + `LiabilitySchedulerService.java:103-108` | `paymentAmount` is validated nowhere — no annotation, no service rule, no DB CHECK. A negative amount is subtracted, so `current − (−x)` grows the debt. | `paymentAmount=-200` accepted through the facade; runs produced `1000 → 1200 → 1400`. | One bad request or MCP call creates a debt that ratchets up every night, forever. | `@Positive` + a service invariant + `CHECK (payment_amount > 0)`. |
| H3 | `AssetService.java:107-111` + `UpdateAssetDTO` | `PUT /assets/{id}` is a full replace over an all-optional DTO: a name-only body nulls `liabilityType`, `paymentFrequency`, `paymentAmount`, resets `originalCurrency` to EUR and `isFavorite` to false. | Name-only update: `{AUTOMATIC, MONTHLY, 300.0000, USD, true}` → `{null, null, null, EUR, false}`; the row left the scheduler query and was never paid, while an untouched control liability in the same run was paid. | Every rename through the API or an MCP `updateAsset` disarms the liability and can flip its currency. | Patch semantics for nullable fields, or partial-update DTOs. |
| H4 | `AssetService.java:107-111` + `Snapshot.java` (no currency on the snapshot) | Changing an asset's `originalCurrency` retro-detects the whole history: snapshots are re-read through the asset's current currency while their stored rate stays behind. | Asset USD 2000, stored rate 0.900000, DTO `amountBaseCurrency=1800`; after USD→GBP the DB row is unchanged but the DTO reads `rate=1.200000, amountBase=2400`; net worth 1350 → 1950. | Net worth, chart and every KPI jump by an FX factor with no data change. | Store the currency on the snapshot, or forbid the change once snapshots exist. |
| H5 | `LiabilitySchedulerService.java:103-108` | A payment larger than the balance is clamped to 0 and the shortfall is dropped with no record. | balance 100, `paymentAmount=720` → stored `0.0000`; log says only "reached zero value". 620 of principal vanished. | Silent loss of debt on refinancing/rounding, and the schedule keeps charging a paid-off liability at 0. | Record the applied amount and cap the payment, or reject the config. |
| H6 | `SnapshotValidationService.java:19-51` + `LiabilitySchedulerService.java:81-90` | `referenceDate` has no upper bound and is a valid write for an AUTOMATIC liability; the cron anchors on the max date, so one future row freezes payments. | Future snapshot `2027-11-14` accepted via `SnapshotFacade`; runs on 2026-11-10 and 2026-12-10 wrote nothing, each logged `1 processed`. | An automatic liability can be frozen indefinitely by a single API call or MCP tool. | Reject future dates (or cap them at today) for automatic liabilities. |
| H7 | `SnapshotService.java:104-105` + same month-upsert | WEEKLY is unrepresentable: four weekly payments collapse into one row whose date moves, so no weekly history exists. | 1000 → 900 → 800 → 700 → 600 over four weeks, but the final table holds 2 rows (`2026-01-26/700`, `2026-02-02/600`); no row ever shows `2026-01-05/1000`. | The chart shows one cliff per month instead of the real amortisation, and the deleted weeks cannot be recovered. | One row per payment, keyed by (asset, due date). |
| H8 | `AssetRepository.java:18-19` vs `AnalyticsService.java:371,389` | Two competing sources of truth: the scheduler selects on `asset.liability_type`, analytics classifies on `category.type` — and `category.type` is user-mutable. | `AUTOMATIC` asset in an ASSET category + `liabilityType=null` asset in a LIABILITY category → analytics reports `gross 500 / liabilities 700 / net −200`; the scheduler query returns the AUTOMATIC one. | A liability can be decremented every month while being counted as an asset (and vice versa), with no UI showing the mismatch. | One discriminator: derive the category type from the assets, or drop `category.type` from the analytics. |
| H9 | `AnalyticsService.java:490-497` | `currencyImpact` adds `amount × Δrate` with a positive sign for liabilities too: a debt whose currency appreciates is reported as a gain. | USD liability 1000, rate 0.90→1.00 → `currencyImpact=100.0000000000` (net worth actually fell 100); asset+liability variant → `200.0000000000`. | The dashboard attributes a loss to FX gains, hiding the loss. | Negate the term for liability assets. |
| H10 | `AnalyticsService.java:417-421` | `debtToAssetRatio` divides by `liabilities + grossAssets` instead of `grossAssets`. | 100000 assets / 40000 debt → `28.5700`, expected `40`. | The headline risk KPI understates leverage and can never reach 100%. | `liabilities / grossAssets`; the frontend fixture already assumes the right formula. |
| H11 | `AnalyticsService.java:444-467` | `assetAllocation` takes `abs()` of every snapshot, so debt becomes a slice of "Asset allocation". | 100000 asset + 40000 mortgage → `[Cash & Bank Accounts=71.43, Mortgage=28.57]`. | Debt is presented as owned value in the pie and in every percentage built on it. | Exclude liability categories from allocation instead of abs-ing them. |
| H12 | `AssetCategoryService.java:127` + `2026-03-18-initial-schema.sql:22` | Deleting a category cascades (JPA `CascadeType.ALL` + DB `ON DELETE CASCADE`) to every asset and snapshot in it; the endpoint returns 204 with no count and no 409. | Schema: `assets.category_id → asset_categories ON DELETE CASCADE`, `snapshots.asset_id → assets ON DELETE CASCADE` (verified with `\d assets`). | One "tidy categories" action silently destroys liabilities and their whole payment history. | Refuse the delete when the category has assets, or move them first. |
| H13 | `McpScopes.java:75-77` + `application.properties:27-28` | Scope enforcement is enabled only when the scope list is non-empty; both defaults are empty, so in the shipped configuration every MCP write tool is callable by any token with the MCP audience. | Config evidence only — not reproduced end to end (needs a live token); the guard is a no-op by construction when the properties are unset. | Any MCP-audience token can create/update liabilities and snapshots. | Fail closed: refuse to start (or deny writes) when the scope lists are empty. |
| H14 | `aurum-fe/src/features/assets/asset-list.tsx:160` → `api.ts:158` → `POST /snapshots` | The inline "current value" editor posts a snapshot for *any* asset, AUTOMATIC liabilities included, while the history sheet deliberately hides manual entry for them; the backend upserts by month (C3). | Code path verified; the backend half is proven in C3. | The one screen with no "automatic" affordance is the one that silently overwrites the cron's row and resets its due date. | Hide or disable the inline editor for AUTOMATIC liabilities. |

## Findings — MEDIUM

| id | where | what is wrong | evidence |
|----|-------|---------------|----------|
| M1 | `AssetValidationService.java:11-36`, `2026-03-20-add-liability-type-fields.sql:5-7` | No liability invariants anywhere: AUTOMATIC without amount/frequency, MANUAL with an amount, negative amounts and MANUAL→AUTOMATIC flips are all accepted; the columns are bare `VARCHAR(20)`. | `AUTOMATIC` with only `paymentAmount` created with 0 snapshots and never paid. Production already holds 12 rows of a shape the code does not validate: `MANUAL` + `MONTHLY` + `payment_amount=0`, produced by the form and rejected by nothing. |
| M2 | `Snapshot.java:27-53` | No `source`/`origin` column: a cron row and a user row are indistinguishable in the API, the UI and the GDPR export, which is also why C3 cannot be corrected in place. | Schema `\d snapshots` has no provenance column. |
| M3 | `LiabilitySchedulerService.java:134` | `plusMonths(1)` clamps 31 → 28, and the run date becomes the new anchor: under the real daily cron the payment day drifts permanently. | Daily runs from a 2026-01-31 snapshot landed on `01-31, 02-28, 03-28, 04-28, 05-28`. (Running only on month-ends hides it.) |
| M4 | `SnapshotService.java:145-160` | No lock or `@Version` on the month row: a user POST and the cron writing the same month are last-write-wins. | Code; the upsert is a read-then-write with no optimistic locking. |
| M5 | `RateBook.java:36` + `ExchangeRateService` | A date with no quote silently falls back to the last known quote, however old. | A 2026-11-14 read of BGN used the 2025-12-31 quote (0.5113) and returned a plausible number; a range with no quotes at all raises `ExchangeRateUnavailableException`. |
| M6 | `ExchangeRateConfig.java:14` | The `RestClient` has no connect/read timeout and is called inside the write transaction: a hung provider stalls the batch with a DB connection open. | Code. |
| M7 | `AssetService.java:72` + `LiabilitySchedulerService.java:72-79` | An AUTOMATIC liability created without `initialValue`/`referenceDate` has no snapshot and is skipped forever, silently. | Created with 0 snapshots; two runs each logged `1 processed`, wrote nothing. |
| M8 | `AnalyticsService.java:446-448,511-517` | Allocation, top assets and favourites use `max(referenceDate)` without `≤ asOf`, so a future-dated snapshot is used as today's value while net worth correctly excludes it. | Code; consistent with H6's accepted future dates. |
| M9 | `AnalyticsService.java:93` | `totalLiabilities` is derived as `gross − net`, so it cannot be validated independently and inherits every asymmetry of the two earlier passes. | A stale liability reports `0.0000` instead of 40000 (C4). |
| M10 | `AssetRepository.java:18-19`, migrations | No index on `assets.liability_type`; the nightly query fetch-joins the complete snapshot history of every user inside a write transaction. | `\d assets` shows only PK and `idx_assets_user_id`. |
| M11 | `AssetMapper.java:133`, `SnapshotMapper.java:54` | A null/legacy `originalCurrency` reaches `Map.copyOf`-backed `RateBook.rate` with a null key → NPE, so one legacy row turns `/assets` and the top-assets tile into a 500. | Code path; analytics itself has a base-currency fallback, the mappers do not. |
| M12 | `TargetMapper.java:93`, `TargetService.java:111-137` | NET_WORTH progress is clamped at 0, so a negative net worth reads as 0 %; `checkAndSyncCompletion` mutates a detached entity on read paths and never persists the completion. | net −800 → `completionPercentage=0.0`. |
| M14 | `LiabilitySchedulerService.java:34` (no run record) | A run leaves no durable trace: no line at startup saying the job is registered, no run table, no actuator endpoint, no health indicator, and a skipped asset logs nothing. A cron that never fires and a cron that fires and does nothing are indistinguishable. | Startup log is 62 lines with no scheduling/scheduler line; the schema has no run-marker table; the prod grep returns nothing, which proves nothing because prod has zero AUTOMATIC liabilities. |
| M13 | `aurum-fe/.../assets.utils.ts:20`, `asset-list.tsx:121` | Group totals sum `latestValueBase` (user currency) but are rendered unlabelled next to rows shown in each asset's own currency. | Code; a foreign-currency liability reads as base currency in the group header. |

## Findings — LOW

| id | where | what is wrong |
|----|-------|---------------|
| L1 | `LiabilitySchedulerService.java:99-101,126-128` | A liability at 0 value is re-selected, re-skipped and re-"processed" every night forever; "reached zero" is logged once and nothing marks it settled. |
| L2 | `AnalyticsService.java:247` | `getProjections` ignores its `years` argument (always 1/5/10) though the controller exposes it. |
| L3 | `AnalyticsService.java:103-109` | `oldestSnapshotYear` comes from the truncated 14-month window, so the year selector is mis-bounded for older accounts. |
| L4 | `AnalyticsService.java:198,216` | Favourite series are keyed by asset name: two same-named assets merge into one series, an asset and a liability with the same name get mixed signs. |
| L5 | `AnalyticsServiceTest.java:340` | The test asserts the wrong debt-ratio formula, so H10 is protected by the suite. |
| L6 | `SnapshotService.java:120,145`, `SnapshotMapper.java:34-42` | Dead code (`save`, `findById`, `findByDateRange`) carrying ownership checks (or skipping them when `userId == null`) that never run — a trap for the next caller. |
| L7 | `LiabilitySchedulerServiceTest.java:95,108` | The suite mocks `SnapshotService`, so the overwrite/atomicity it "protects" is never exercised, and `minusMonths(2) → 800` enshrines the missed-run bug from C2 as expected behaviour. |

## What I verified and it is fine

- **Idempotency within a day**: two runs on the same date charge once; the second is stopped by the due check.
- **MANUAL liabilities**: never selected by the scheduler, never decremented.
- **Ownership**: no IDOR on any asset/snapshot/liability path — every endpoint and MCP tool checks the owner in the service or validation layer (one leaky 400-vs-403 status, not exploitable). Two-user probe found no cross-tenant read or write.
- **Sign convention in the simple case**: liabilities are subtracted exactly once and `totalGrossAssets` does not contain them; `net = gross − liabilities` holds. `assetsOnly` and the chart's `totalAssetsOnly` agree with `grossAssets`.
- **Multi-currency sums**: conversion happens per snapshot before summing — no unconverted sum found (EUR 1000 + USD 1000@0.92 + USD 500 debt → gross 1920, liabilities 460, net 1460, arithmetically correct).
- **Provider failure inside the loop**: leaves no half-written row, and a healthy asset in the same batch is still paid (`1 processed, 1 failed`) — the rollback in C1 needs a persistence-level failure, not a lookup failure.
- **Cron shape**: `0 0 0 * * *` UTC with a `Clock.systemUTC()` bean, `@EnableScheduling` present — the job is live, not dead code.
- **Disabled/deleted users**: there is no enabled/deleted flag any more (`2026-04-03-remove-isactive.sql`); a deleted user cascades away, so there is nothing for the scheduler to skip.
- **Cashflow does not double-count**: no cashflow code reads `paymentAmount` or writes snapshots, so a payment is not booked twice — and also never appears as "spent" (a coherence gap, not a bug).
- **GDPR export**: includes liability assets and their snapshots.

## Is the scheduler observable?

No — and the production evidence is that **nothing in production can tell a jammed cron from a
quiet one**. Take each artifact that would exist if the job ran:

1. **A registration line at startup.** `@Scheduled` registration logs nothing at INFO; the 62-line
   startup log of the running app contains no scheduling or scheduler line. Absent.
2. **The two INFO lines at 00:00 UTC** (`Running automatic liability payment processor...` and the
   summary). This is the only trace that exists at all, it is not retained across a container
   restart, and prod's logs hold 48 minutes. Absent by retention.
3. **A `snapshots` row dated on the run day** for every automatic liability. Production has zero
   `AUTOMATIC` liabilities, so its absence proves nothing either way. Uninformative.
4. **A run record**: a table, an actuator endpoint, a health indicator, a metric. The development
   database has no run-marker table and `management.endpoints.web.exposure.include=health,info`
   exposes none. Absent.

So the honest statement is: production cannot demonstrate that the cron has ever fired. The one
trace it would leave over-counts skips (H1), can be printed for a run that then rolls back entirely
(C1), and says nothing about the assets it skipped (zero log lines). Its unit test is not a safety
net either: the only collaborator that matters (`SnapshotService`) is mocked, WEEKLY/YEARLY/null/
negative are uncovered, and the missed-run behaviour is asserted as correct (L7).

## State

Nothing was committed, no branch, no test file. Every probe ran on a throwaway database, and the
developer's local database was left untouched — restored to its previous contents afterwards.

**Which schema the probes ran on.** The probe database was built by replaying the changelog SQL files
in master order — the same input production's Liquibase ran — because a plain boot creates nothing
(see H15). An earlier version of this section said the developer's database has no `databasechangelog`
table, was created by some other means and is therefore a second, untracked schema whose constraints
are not guaranteed to match either production or the changelog. That was wrong, and the cause is now
known: for the duration of the audit the database *named* `aurum` held a throwaway probe database
while the real data was parked under another name, so a `databasechangelog` lookup against that name
found nothing, and the absence was read as a property of the developer's database instead of a
property of the probe. As it stands, the developer's database carries `databasechangelog` with its own
execution history — the rows that predate this change keep their April 2026 `dateexecuted` values, and
the two changesets added with this change were applied to that database during this change's
verification — so it is a migrated schema, not a hand-built one. A metadata diff of it against a
database built from nothing by replaying the changelog is empty on tables, columns, types, nullability,
defaults, primary-key, unique, foreign-key and check constraints, and index definitions; the only
residue is the auto-generated NOT NULL constraint names Postgres derives per database. The
finding-level evidence is unaffected: C1, C3 and H2 were reproduced on the replay-built database, and
that remains the right place to reproduce a constraint-dependent bug — because the developer's data
must not be a test fixture, not because its schema differs.
