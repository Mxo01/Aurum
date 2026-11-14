import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TooltipProvider } from "@/components/ui/tooltip";
import { FavoriteToggle } from "./favorite-toggle";

function renderToggle(pressed: boolean, onPressedChange = vi.fn()) {
	return render(
		<TooltipProvider>
			<FavoriteToggle
				pressed={pressed}
				onPressedChange={onPressedChange}
				label="Mark as favorite"
			/>
		</TooltipProvider>
	);
}

describe("FavoriteToggle", () => {
	it("should toggle the pressed state", async () => {
		// GIVEN
		const onPressedChange = vi.fn();
		const user = userEvent.setup();
		renderToggle(false, onPressedChange);

		// WHEN
		await user.click(screen.getByRole("button", { name: "Mark as favorite" }));

		// THEN
		expect(onPressedChange).toHaveBeenCalledWith(true);
	});

	it("should explain the tracked state", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderToggle(true);

		// WHEN
		await user.hover(screen.getByRole("button", { name: "Mark as favorite" }));

		// THEN
		expect(await screen.findByRole("tooltip")).toHaveTextContent("Shown in the dashboard chart");
	});

	it("should explain the untracked state", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderToggle(false);

		// WHEN
		await user.hover(screen.getByRole("button", { name: "Mark as favorite" }));

		// THEN
		expect(await screen.findByRole("tooltip")).toHaveTextContent(
			"Not shown in the dashboard chart"
		);
	});
});
