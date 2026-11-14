import { describe, expect, it } from "vitest";
import { router } from "./router";

describe("router", () => {
	it("should expose the public and protected routes", () => {
		// GIVEN
		const testSubject = router;
		const root = testSubject.routes[0];
		const children = root?.children ?? [];
		const protectedRoute = children.find(route => route.children);

		// THEN
		expect(root).toBeDefined();
		expect(children.map(route => route.path)).toEqual(
			expect.arrayContaining(["/", "/privacy", "*"])
		);
		expect((protectedRoute?.children ?? []).map(route => route.path)).toEqual(
			expect.arrayContaining(["dashboard", "assets", "cashflow", "profile", "assets/categories"])
		);
	});

	it("should lazy load every page", async () => {
		// GIVEN
		const root = router.routes[0];

		// WHEN
		const children = root?.children ?? [];
		const protectedRoute = children.find(route => route.children);
		const lazyRoutes = [...children, ...(protectedRoute?.children ?? [])].filter(
			route => route.lazy
		);
		const modules = await Promise.all(
			lazyRoutes.map(route =>
				typeof route.lazy === "function" ? route.lazy() : Promise.resolve(undefined)
			)
		);

		// THEN
		expect(modules.length).toBeGreaterThan(0);
		for (const module of modules) {
			expect(module).toHaveProperty("Component");
		}
	});

	it("should redirect the legacy categories path and unknown paths", () => {
		// GIVEN
		const root = router.routes[0];
		const children = root?.children ?? [];
		const protectedRoute = children.find(route => route.children);
		const categories = (protectedRoute?.children ?? []).find(
			route => route.path === "assets/categories"
		);
		const fallback = children.find(route => route.path === "*");

		// THEN
		expect(categories?.element).toBeDefined();
		expect(fallback?.element).toBeDefined();
	});
});
