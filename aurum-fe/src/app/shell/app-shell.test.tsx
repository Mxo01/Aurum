import { render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { AppShell } from "./app-shell";

vi.mock("./top-bar", () => ({ TopBar: () => <div>top bar</div> }));
vi.mock("./bottom-tabs", () => ({ BottomTabs: () => <div>bottom tabs</div> }));
vi.mock("./command-palette", () => ({ CommandPalette: () => <div>command palette</div> }));

describe("AppShell", () => {
	it("should render the shell around the active route", () => {
		// GIVEN
		const testSubject = AppShell;
		const Subject = testSubject;
		const router = createMemoryRouter(
			[
				{
					element: <Subject />,
					children: [{ index: true, element: <div>page content</div> }]
				}
			],
			{ initialEntries: ["/"] }
		);

		// WHEN
		render(<RouterProvider router={router} />);

		// THEN
		expect(screen.getByText("top bar")).toBeInTheDocument();
		expect(screen.getByText("bottom tabs")).toBeInTheDocument();
		expect(screen.getByText("command palette")).toBeInTheDocument();
		expect(screen.getByText("page content")).toBeInTheDocument();
	});
});
