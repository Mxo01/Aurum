import { render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { RouteError } from "./route-error";

describe("RouteError", () => {
	it("should render the status of a route response error", async () => {
		// GIVEN
		const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => undefined);
		const router = createMemoryRouter([
			{
				path: "/",
				element: <p>ok</p>,
				loader: () => {
					throw new Response("Not found", { status: 404, statusText: "Not Found" });
				},
				errorElement: <RouteError />
			}
		]);

		// WHEN
		render(<RouterProvider router={router} />);

		// THEN
		expect(await screen.findByText("404 Not Found")).toBeInTheDocument();
		consoleSpy.mockRestore();
	});

	it("should render the message of a generic error", async () => {
		// GIVEN
		const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => undefined);
		const router = createMemoryRouter([
			{
				path: "/",
				element: <p>ok</p>,
				loader: () => {
					throw new Error("kaboom");
				},
				errorElement: <RouteError />
			}
		]);

		// WHEN
		render(<RouterProvider router={router} />);

		// THEN
		expect(await screen.findByText("kaboom")).toBeInTheDocument();
		consoleSpy.mockRestore();
	});
});

describe("RouteError fallback", () => {
	it("should show the generic message for a non-Error value", async () => {
		// GIVEN
		const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => undefined);
		const router = createMemoryRouter([
			{
				path: "/",
				element: <p>ok</p>,
				loader: () => {
					throw "kaboom";
				},
				errorElement: <RouteError />
			}
		]);

		// WHEN
		render(<RouterProvider router={router} />);

		// THEN
		expect(
			await screen.findByText("An unexpected error interrupted the page.")
		).toBeInTheDocument();
		consoleSpy.mockRestore();
	});
});
