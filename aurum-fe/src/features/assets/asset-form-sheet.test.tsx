import { fireEvent, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { env } from "@/lib/env";
import { buildAsset, buildCategory } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { AssetFormSheet } from "./asset-form-sheet";

const mockCategories = [
	buildCategory({ id: "c1", name: "Investments", type: "ASSET" }),
	buildCategory({ id: "c2", name: "Mortgage", type: "LIABILITY" })
];

describe("AssetFormSheet", () => {
	it("should render the new asset form disabled", () => {
		// GIVEN
		const testSubject = AssetFormSheet;
		const Subject = testSubject;

		// WHEN
		renderWithProviders(
			<Subject
				open
				onOpenChange={() => undefined}
				asset={null}
				categories={mockCategories}
			/>
		);

		// THEN
		expect(screen.getByRole("heading", { name: "New asset" })).toBeInTheDocument();
		expect(screen.getByLabelText("Initial value")).toBeInTheDocument();
		expect(screen.getByText("Mark as favorite")).toBeInTheDocument();
		expect(
			screen.getByText("Favorite assets are plotted on the dashboard's net worth chart.")
		).toBeInTheDocument();
		expect(screen.getByRole("button", { name: "Add asset" })).toBeDisabled();
	});

	it("should create an asset", async () => {
		// GIVEN
		let postedBody: unknown;
		server.use(
			http.post(`${env.apiUrl}/assets`, async ({ request }) => {
				postedBody = await request.json();
				return HttpResponse.json(buildAsset());
			})
		);
		const onOpenChange = vi.fn();
		const testSubject = AssetFormSheet;
		const Subject = testSubject;
		renderWithProviders(
			<Subject
				open
				onOpenChange={onOpenChange}
				asset={null}
				categories={mockCategories}
			/>
		);

		// WHEN
		const user = userEvent.setup();
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Apartment" } });
		await user.click(screen.getByLabelText("Category"));
		await user.click(await screen.findByRole("option", { name: "Investments" }));
		fireEvent.change(screen.getByLabelText("Initial value"), { target: { value: "430000" } });
		await waitFor(() => expect(screen.getByRole("button", { name: "Add asset" })).toBeEnabled());
		fireEvent.submit(
			screen.getByRole("button", { name: "Add asset" }).closest("form") as HTMLFormElement
		);

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).toMatchObject({
			name: "Apartment",
			categoryId: "c1",
			originalCurrency: "EUR",
			initialValue: 430000
		});
		expect(onOpenChange).toHaveBeenCalledWith(false);
	});

	it("should require the automatic payment fields for an automatic liability", async () => {
		// GIVEN
		let postedBody: unknown;
		server.use(
			http.post(`${env.apiUrl}/assets`, async ({ request }) => {
				postedBody = await request.json();
				return HttpResponse.json(buildAsset());
			})
		);
		const testSubject = AssetFormSheet;
		const Subject = testSubject;
		renderWithProviders(
			<Subject
				open
				onOpenChange={() => undefined}
				asset={null}
				categories={mockCategories}
			/>
		);

		// WHEN
		const user = userEvent.setup();
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mortgage" } });
		await user.click(screen.getByLabelText("Category"));
		await user.click(await screen.findByRole("option", { name: "Mortgage" }));

		const liabilityType = await screen.findByLabelText("Liability type");
		await user.click(liabilityType);
		await user.click(await screen.findByRole("option", { name: "Automatic" }));

		const frequency = await screen.findByLabelText("Payment frequency");
		expect(screen.getByLabelText("Payment amount")).toBeInTheDocument();
		await user.click(frequency);
		await user.click(await screen.findByRole("option", { name: "Monthly" }));
		fireEvent.change(screen.getByLabelText("Payment amount"), { target: { value: "500" } });
		await waitFor(() => expect(screen.getByRole("button", { name: "Add asset" })).toBeEnabled());
		fireEvent.submit(
			screen.getByRole("button", { name: "Add asset" }).closest("form") as HTMLFormElement
		);

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).toMatchObject({
			liabilityType: "AUTOMATIC",
			paymentFrequency: "MONTHLY",
			paymentAmount: 500
		});
	});

	it("should edit an asset without exposing the initial value", async () => {
		// GIVEN
		let updatedBody: unknown;
		server.use(
			http.put(`${env.apiUrl}/assets/:id`, async ({ request }) => {
				updatedBody = await request.json();
				return HttpResponse.json(buildAsset());
			})
		);
		const mockAsset = buildAsset({
			id: "a1",
			name: "Apartment",
			categoryId: "c1",
			categoryName: "Investments",
			categoryIcon: null,
			type: "ASSET",
			originalCurrency: "EUR"
		});
		const testSubject = AssetFormSheet;
		const Subject = testSubject;
		renderWithProviders(
			<Subject
				open
				onOpenChange={() => undefined}
				asset={mockAsset}
				categories={mockCategories}
			/>
		);

		// WHEN
		expect(screen.getByRole("heading", { name: "Edit asset" })).toBeInTheDocument();
		expect(screen.queryByLabelText("Initial value")).not.toBeInTheDocument();
		expect(screen.getByLabelText("Currency")).toBeDisabled();

		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Apartment Berlin" } });
		await waitFor(() => expect(screen.getByRole("button", { name: "Save changes" })).toBeEnabled());
		fireEvent.submit(
			screen.getByRole("button", { name: "Save changes" }).closest("form") as HTMLFormElement
		);

		// THEN
		await waitFor(() => expect(updatedBody).toBeDefined());
		expect(updatedBody).toMatchObject({ name: "Apartment Berlin", categoryId: "c1" });
	});
});

describe("AssetFormSheet details", () => {
	it("should show the selected category and close with cancel", async () => {
		// GIVEN
		const onOpenChange = vi.fn();
		const testSubject = AssetFormSheet;
		const Subject = testSubject;
		renderWithProviders(
			<Subject
				open
				onOpenChange={onOpenChange}
				asset={null}
				categories={mockCategories}
			/>
		);

		// WHEN
		await userEvent.click(screen.getByLabelText("Category"));
		await userEvent.click(screen.getByRole("option", { name: "Investments" }));

		// THEN
		expect(screen.getByLabelText("Category")).toHaveTextContent("Investments");

		// WHEN
		await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

		// THEN
		expect(onOpenChange).toHaveBeenCalledWith(false);
	});
});

it("should change the currency and the favorite flag", async () => {
	// GIVEN
	const testSubject = AssetFormSheet;
	const Subject = testSubject;
	renderWithProviders(
		<Subject
			open
			onOpenChange={() => undefined}
			asset={null}
			categories={mockCategories}
		/>
	);

	// WHEN
	await userEvent.click(screen.getByLabelText("Currency"));
	await userEvent.click(screen.getByRole("option", { name: "USD" }));
	await userEvent.click(screen.getByRole("button", { name: "Mark as favorite" }));

	// THEN
	expect(screen.getByLabelText("Currency")).toHaveTextContent("USD");
	expect(screen.getByRole("button", { name: "Mark as favorite" })).toHaveAttribute(
		"aria-pressed",
		"true"
	);
});

it("should default a new liability to manual", async () => {
	// GIVEN
	let postedBody: unknown;
	server.use(
		http.post(`${env.apiUrl}/assets`, async ({ request }) => {
			postedBody = await request.json();
			return HttpResponse.json(buildAsset());
		})
	);
	const testSubject = AssetFormSheet;
	const Subject = testSubject;
	renderWithProviders(
		<Subject
			open
			onOpenChange={() => undefined}
			asset={null}
			categories={mockCategories}
		/>
	);

	// WHEN
	const user = userEvent.setup();
	fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Mortgage" } });
	await user.click(screen.getByLabelText("Category"));
	await user.click(await screen.findByRole("option", { name: "Mortgage" }));
	expect(await screen.findByLabelText("Liability type")).toBeInTheDocument();
	fireEvent.submit(
		screen.getByRole("button", { name: "Add asset" }).closest("form") as HTMLFormElement
	);

	// THEN
	await waitFor(() => expect(postedBody).toBeDefined());
	expect(postedBody).toMatchObject({ liabilityType: "MANUAL" });
});

it("should show the required field errors", async () => {
	// GIVEN
	const testSubject = AssetFormSheet;
	const Subject = testSubject;
	renderWithProviders(
		<Subject
			open
			onOpenChange={() => undefined}
			asset={null}
			categories={mockCategories}
		/>
	);

	// WHEN
	fireEvent.submit(
		screen.getByRole("button", { name: "Add asset" }).closest("form") as HTMLFormElement
	);

	// THEN
	expect(await screen.findByText("Name is required")).toBeInTheDocument();
	expect(screen.getByText("Category is required")).toBeInTheDocument();
});
