# React migration — decisions and swap checklist

Status: migration in flight. Angular stays on `main` and on `:4200`; the React
app lives in `aurum-fe-react/` on `:4300`. Nothing merges until the user's
explicit OK.

## Decisions

- One integration branch `refactor/react-frontend`. Slices are commits, not
  branches. No PR until the user asks for it.
- Backend is frozen: no Java, DB, Liquibase, OpenAPI or auth changes.
- Stack: Vite 8 + React 19 + TypeScript 5.9 strict, Tailwind 4, shadcn/ui,
  React Router, TanStack Query, Zustand, openapi-fetch + openapi-typescript,
  Auth0 React SDK, react-hook-form + zod, Recharts, Vitest/RTL/MSW, Playwright.
- Design: warm neutral + gold accent, Geist Sans/Mono, no glass/glow/dot-grid,
  data-first pages, top bar on desktop and bottom tabs on mobile.
- Coverage gate: 100% statements/branches/functions/lines, excluding only
  generated or vendored files.
- End of migration: push, run the app, walkthrough. **No PR** until the user
  says so.

## Slices

- [x] 0. Scaffold, shell, router, auth, landing, legal, tooling.
- [ ] 1. Typed API layer, query hooks, MSW fixtures, error boundary.
- [ ] 2. Dashboard.
- [ ] 3. Assets, snapshots, categories.
- [ ] 4. Cash flow.
- [ ] 5. Targets, profile, MCP, GDPR.
- [ ] 6. Shortcuts, ⌘K, polish, swap, docs, CI.

## Public configuration

All frontend values are public build-time configuration:

```
VITE_API_URL, VITE_AUTH0_DOMAIN, VITE_AUTH0_CLIENT_ID, VITE_AUTH0_AUDIENCE
```

No secret is involved. Local login uses `http://localhost:4200` (Angular) and,
for side-by-side review, `http://localhost:4300` (React). Auth0 does not accept
wildcards: the `:4300` callback must be added manually for a real login on the
new app. That is a review convenience, not a merge requirement.

## Swap checklist

Everything that must be touched when the React app replaces Angular. Keep this
list as the single source of truth for the swap.

### A. Code and repository

- [ ] Parity verified on every route, e2e green, no TODO.
- [ ] Tag `angular-final` on `main` before the merge.
- [ ] `git rm -r aurum-fe` and `git mv aurum-fe-react aurum-fe`.
- [ ] Vite `server.port` 4200 with `strictPort` (same local origin, same Auth0
      callback), `preview` on 4200 too.
- [ ] Remove Angular leftovers: `angular.json`, `src/environments/`, `.angular/`,
      `dist/aurum-fe`, PrimeNG styles.
- [ ] `Makefile`: `stop` uses `pkill -f vite`; `start` unchanged.
- [ ] Root `.gitignore`: keep `.opencode/`, add `.env*.local`, confirm `dist/`
      and `coverage/`.
- [ ] `README.md`: stack, layout, architecture diagram, commands, `api:types`
      path, testing.
- [ ] Root `AGENTS.md`: merge `aurum-fe-react/AGENTS.md` into the frontend
      sections, remove Angular/PrimeNG/ng-mocks/TestBed/RxJS rules, update SEO
      paths, correct the "environment.ts is gitignored" note (it is tracked).
- [ ] `docs/security-audit.md`: update the frontend stack line or mark it as
      historical.
- [ ] Delete this file once the swap is done (history keeps it).

### B. CI

- [ ] `.github/workflows/ci.yml` frontend job: `working-directory: aurum-fe`,
      `cache-dependency-path: aurum-fe/package-lock.json`.
- [ ] Test command `npm test -- --run` (Vitest).
- [ ] Add `npm run lint`, `npm run typecheck`, `npm run build`.
- [ ] Coverage path `aurum-fe/coverage`; upload lcov.
- [ ] Node 22 (Vite 8 needs >= 20.19 / 22.12).
- [ ] `deploy.yml` unchanged: it only watches `aurum-be/**`.

### C. Vercel (manual dashboard)

- [ ] Project root stays `aurum-fe` after the rename.
- [ ] Framework preset: Vite.
- [ ] Build command `npm run build`; output directory `dist`.
- [ ] Install `npm ci`; Node 22.x.
- [ ] Env vars: `VITE_API_URL`, `VITE_AUTH0_DOMAIN`, `VITE_AUTH0_CLIENT_ID`,
      `VITE_AUTH0_AUDIENCE` (values already public today).
- [ ] `vercel.json` rewrite `/(.*) → /index.html` still applies.
- [ ] Verify deep links after the first deploy.

### D. Auth0

- [ ] No change expected: callback `http://localhost:4200` and
      `https://aurum-networth.com`, logout URLs and web origins stay the same.
- [ ] Same SPA client and audience: backend tokens unchanged.
- [ ] Optional: add a specific preview URL for real-login previews (no wildcards
      supported).

### E. Secrets

- [ ] No rotation: the SPA client ID, domain, audience and API URL are public.
- [ ] Never commit `.env.local`; keep `.env.example`.
- [ ] Backend secrets untouched.

### F. Post-swap verification

- [ ] Login, silent refresh, logout.
- [ ] Dashboard, Assets, Cash flow, Profile, Privacy, 404.
- [ ] Hard refresh on a deep link.
- [ ] Dark/light, privacy mode, currency/locale.
- [ ] CRUD smoke: asset → snapshot → value edit → delete; cashflow edit; target
      create/delete; JSON export.
- [ ] Lighthouse mobile/desktop, bundle report, zero console errors.
- [ ] Rollback: revert the merge commit on `main`; Vercel redeploys Angular.
