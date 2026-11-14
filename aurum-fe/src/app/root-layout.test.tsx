import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { beforeEach, describe, expect, it } from "vitest";
import { RootLayout } from "./root-layout";

describe("RootLayout", () => {
	beforeEach(() => {
		window.localStorage.clear();
	});

	it("should render the outlet and the consent banner", () => {
		// GIVEN
		const testSubject = RootLayout;
		const Subject = testSubject;

		// WHEN
		render(
			<MemoryRouter initialEntries={["/"]}>
				<Routes>
					<Route element={<Subject />}>
						<Route
							index
							element={<div>page content</div>}
						/>
					</Route>
				</Routes>
			</MemoryRouter>
		);

		// THEN
		expect(screen.getByText("page content")).toBeInTheDocument();
		expect(screen.getByText(/Aurum stores your financial data/)).toBeInTheDocument();
	});
});
