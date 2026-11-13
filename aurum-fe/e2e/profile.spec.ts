import { expect, test } from "./fixtures/aurum";

test("shows the profile tabs and the MCP config", async ({ page }) => {
	await page.goto("/profile");

	await expect(page.getByRole("heading", { name: "Profile" })).toBeVisible();
	await page.getByRole("tab", { name: "Integrations" }).click();
	await expect(page.getByText(/mcpServers/)).toBeVisible();

	await page.getByRole("tab", { name: /Data/ }).click();
	await expect(page.getByRole("button", { name: "Export JSON" })).toBeVisible();
	await expect(page.getByText("Danger zone")).toBeVisible();
});

test("requires the typed confirmation to delete the account", async ({ page }) => {
	await page.goto("/profile");
	await page.getByRole("tab", { name: /Data/ }).click();
	await page.getByRole("button", { name: "Delete account" }).click();

	const dialog = page.getByRole("alertdialog");
	const confirm = dialog.getByRole("button", { name: "Delete account" });
	await expect(confirm).toBeDisabled();

	await dialog.getByLabel("Type DELETE to confirm").fill("DELETE");
	await expect(confirm).toBeEnabled();
});
