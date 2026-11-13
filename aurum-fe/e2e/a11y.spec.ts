import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "./fixtures/aurum";

for (const path of ["/dashboard", "/assets", "/cashflow", "/profile"]) {
	test(`has no serious accessibility issues on ${path}`, async ({ page }) => {
		await page.goto(path);
		await page.waitForLoadState("networkidle");

		const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();

		const blocking = results.violations.filter(
			violation => violation.impact === "critical" || violation.impact === "serious"
		);

		expect(blocking.map(violation => `${violation.id}: ${violation.nodes.length}`)).toEqual([]);
	});
}
