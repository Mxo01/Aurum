# AGENTS.md — aurum-fe-react

Project instructions for the React frontend. The shared baseline lives in the
user-level `AGENTS.md` and applies here in full; this file adds the
framework-specific detail and wins on conflict. This file replaces the old
Angular/PrimeNG rules when the swap lands.

## Stack

- React 19 + Vite 8 + TypeScript 5.9 (strict).
- Tailwind CSS 4 + shadcn/ui (Radix) + `lucide-react` + `sonner`.
- React Router (data mode, lazy routes) + TanStack Query 5 + Zustand 5.
- `openapi-fetch` with types generated from `../aurum-be/openapi.json`.
- `@auth0/auth0-react` (same SPA tenant/client as the Angular app).
- `react-hook-form` + `zod`; Recharts 3 for charts.
- Vitest + React Testing Library + MSW; Playwright + axe for end-to-end.

## Commands

```bash
npm run dev            # Vite on :4300 (Angular keeps :4200 during the migration)
npm run build          # typecheck + production build
npm run preview        # production preview on :4300
npm run lint           # ESLint
npm run typecheck      # tsc --noEmit
npm run test           # Vitest watch
npm run test:run       # Vitest once
npm run test:coverage  # Vitest + v8 coverage
npm run test:e2e       # Playwright
npm run api:types      # regenerate src/lib/api/schema.d.ts from the backend OpenAPI
```

## Architecture

```
src/
  app/          providers, router, shell (top bar, bottom tabs, command palette)
  components/   ui (shadcn, vendored) + app (Money, Delta, PageHeader, EmptyState)
  features/     auth, dashboard, assets, categories, snapshots, cashflow,
                targets, profile, legal, landing — each with page/, components, api.ts
  lib/          api (client, keys, types, schema), format, stores, utils
  styles/       Tailwind 4 tokens
  test/         setup, render helper, MSW handlers and fixtures
```

Rules:

- A feature never imports from another feature. Shared code goes to `lib/` or
  `components/`.
- Pages do not call `fetch`. They use the query/mutation hooks in the feature's
  `api.ts`; hooks use `useApi()`.
- Server state lives in TanStack Query only. Client state lives in Zustand
  (theme, privacy, command palette) or the URL (year, filters). No Redux.
- Query keys are centralized in `lib/api/keys.ts`. Every mutation invalidates
  the keys it actually changes.
- `components/ui/**` is vendored shadcn code: use it, don't fork it. App-specific
  composition goes to `components/app/**` or inside the feature.

## UI rules

- Warm neutral palette with a gold accent used sparingly. Semantic green/red
  only for money movements.
- No backdrop blur, decorative gradients, glows, dot grids, serif italics,
  pulsing badges or uppercase tracking. Radius 10–12px, 1px borders.
- Numbers use the `num` utility (Geist Mono + tabular figures). Money is always
  formatted through `usePreferences().money`, never with ad-hoc `toFixed`.
- Every screen has one primary action; details open in a sheet, not a new page.
- Accessible by construction: real buttons, labels for every input, visible
  focus ring, `aria-label` on icon-only controls, keyboard operable menus and
  dialogs, `prefers-reduced-motion` respected.

## Testing

This app starts from zero, so the gate is **100% coverage** of statements,
branches, functions and lines. The only exclusions are generated or vendored
files: `src/components/ui/**`, `src/lib/api/schema.d.ts`, `src/main.tsx` and
type declaration files. No exclusion for a component that is just "hard".

- Unit and integration: Vitest + RTL + MSW. Mock every external dependency:
  network, Auth0, clock, storage. A unit test never touches the real world.
- Assign the unit under test to `testSubject`.
- Build inputs with factories/builders and named fixtures; assert against named
  expectations. Table-driven tests over copy-pasted cases.
- No snapshot tests. No `any`. Comments only for `// GIVEN`, `// WHEN`, `// THEN`.
- Query hooks are tested with `renderHook` + MSW, including the error path.
- Components are tested through accessible roles and labels, not test IDs, unless
  no accessible alternative exists.
- E2E (Playwright) runs against the production build with a stateful in-memory
  API mock and a network-stubbed Auth0. Deterministic clock, no `waitForTimeout`,
  fresh context per test, axe scan on the main pages.

## Definition of done

- `lint` + `typecheck` + `test:coverage` + `build` + `test:e2e` green.
- New behaviour covered by tests in the same change, including failure paths.
- No dead code, no debug output, no TODO.
- README (root) and this file describe the code that actually exists.

## SEO

Route changes must update both `public/robots.txt` and `public/sitemap.xml`, as
in the legacy app.
