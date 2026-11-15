import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { createElement } from "react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { env } from "@/lib/env";
import { MESSAGE } from "@/lib/validation";
import { buildCategory } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { CategoriesSheet } from "./categories-sheet";

const mockCategories = [
	buildCategory({ id: "c1", name: "Real Estate", icon: "pi-home", isDefault: true }),
	buildCategory({ id: "c2", name: "Loans", type: "LIABILITY", icon: "pi-credit-card" })
];

function useCategoriesFixture() {
	server.use(http.get(`${env.apiUrl}/categories`, () => HttpResponse.json(mockCategories)));
}

const NAME_INPUT_LABEL = "Name";
const ADD_CATEGORY = "Add category";
const CREATE_CATEGORY = "Create category";

const BLANK_NAMES = [
	{ describe: "an empty name", value: "" },
	{ describe: "a whitespace-only name", value: "   " }
];

function renderCategoriesSheet() {
	useCategoriesFixture();
	const testSubject = CategoriesSheet;
	return renderWithProviders(
		createElement(testSubject, { open: true, onOpenChange: () => undefined })
	);
}

describe("CategoriesSheet", () => {
	it("should list default and custom categories", async () => {
		// GIVEN
		useCategoriesFixture();
		const testSubject = CategoriesSheet;

		// WHEN
		renderWithProviders(createElement(testSubject, { open: true, onOpenChange: () => undefined }));

		// THEN
		expect(await screen.findByText("Real Estate")).toBeInTheDocument();
		expect(screen.getByText("Loans")).toBeInTheDocument();
		expect(screen.queryByRole("button", { name: "Edit Real Estate" })).not.toBeInTheDocument();
		expect(screen.getByRole("button", { name: "Edit Loans" })).toBeInTheDocument();
		expect(screen.getByText("Liability")).toBeInTheDocument();
	});

	it("should create a category", async () => {
		// GIVEN
		useCategoriesFixture();
		let postedBody: unknown;
		server.use(
			http.post(`${env.apiUrl}/categories`, async ({ request }) => {
				postedBody = await request.json();
				return HttpResponse.json(mockCategories[0]);
			})
		);
		const testSubject = CategoriesSheet;
		renderWithProviders(createElement(testSubject, { open: true, onOpenChange: () => undefined }));
		await screen.findByText("Real Estate");

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Add category" }));
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Stocks" } });
		fireEvent.click(screen.getByRole("button", { name: "Create category" }));

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).toMatchObject({ name: "Stocks", type: "ASSET" });
	});

	it("should edit a category", async () => {
		// GIVEN
		useCategoriesFixture();
		let updatedBody: unknown;
		server.use(
			http.put(`${env.apiUrl}/categories/:id`, async ({ request }) => {
				updatedBody = await request.json();
				return HttpResponse.json(mockCategories[1]);
			})
		);
		const testSubject = CategoriesSheet;
		renderWithProviders(createElement(testSubject, { open: true, onOpenChange: () => undefined }));
		await screen.findByText("Loans");

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Edit Loans" }));
		const nameInput = await screen.findByLabelText("Name");
		fireEvent.change(nameInput, { target: { value: "Borrowing" } });
		fireEvent.click(screen.getByRole("button", { name: "Save changes" }));

		// THEN
		await waitFor(() => expect(updatedBody).toBeDefined());
		expect(updatedBody).toMatchObject({ name: "Borrowing" });
	});

	it("should delete a category after confirmation", async () => {
		// GIVEN
		useCategoriesFixture();
		let deleteCalls = 0;
		server.use(
			http.delete(`${env.apiUrl}/categories/:id`, () => {
				deleteCalls += 1;
				return new HttpResponse(null, { status: 200 });
			})
		);
		const testSubject = CategoriesSheet;
		renderWithProviders(createElement(testSubject, { open: true, onOpenChange: () => undefined }));
		await screen.findByText("Loans");

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Delete Loans" }));
		const dialog = await screen.findByRole("alertdialog");
		fireEvent.click(within(dialog).getByRole("button", { name: "Delete" }));

		// THEN
		await waitFor(() => expect(deleteCalls).toBe(1));
	});
});

it("should require a name", async () => {
	// GIVEN
	useCategoriesFixture();
	const testSubject = CategoriesSheet;
	renderWithProviders(createElement(testSubject, { open: true, onOpenChange: () => undefined }));
	await screen.findByText("Real Estate");

	// WHEN
	fireEvent.click(screen.getByRole("button", { name: "Add category" }));
	fireEvent.click(screen.getByRole("button", { name: "Create category" }));

	// THEN
	expect(await screen.findByText("Name is required")).toBeInTheDocument();
});

it("should create a liability with a custom icon", async () => {
	// GIVEN
	useCategoriesFixture();
	let postedBody: unknown;
	server.use(
		http.post(`${env.apiUrl}/categories`, async ({ request }) => {
			postedBody = await request.json();
			return HttpResponse.json(mockCategories[1]);
		})
	);
	const testSubject = CategoriesSheet;
	renderWithProviders(createElement(testSubject, { open: true, onOpenChange: () => undefined }));
	await screen.findByText("Real Estate");

	// WHEN
	fireEvent.click(screen.getByRole("button", { name: "Add category" }));
	fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Borrowing" } });
	fireEvent.click(screen.getByLabelText("Type"));
	fireEvent.click(await screen.findByRole("option", { name: "Liability" }));
	fireEvent.click(screen.getByRole("button", { name: "Home" }));
	fireEvent.click(screen.getByRole("button", { name: "Create category" }));

	// THEN
	await waitFor(() => expect(postedBody).toBeDefined());
	expect(postedBody).toMatchObject({ name: "Borrowing", type: "LIABILITY", icon: "pi-home" });
});

it("should toggle the selected icon off", async () => {
	// GIVEN
	useCategoriesFixture();
	const testSubject = CategoriesSheet;
	renderWithProviders(createElement(testSubject, { open: true, onOpenChange: () => undefined }));
	await screen.findByText("Real Estate");
	fireEvent.click(screen.getByRole("button", { name: "Add category" }));
	const home = screen.getByRole("button", { name: "Home" });

	// WHEN
	fireEvent.click(home);

	// THEN
	expect(home).toHaveAttribute("aria-pressed", "true");

	// WHEN
	fireEvent.click(home);

	// THEN
	expect(home).toHaveAttribute("aria-pressed", "false");
});

describe("CategoriesSheet client validation", () => {
	it("should turn off the browser's own validation on the category form", async () => {
		// GIVEN
		renderCategoriesSheet();
		await screen.findByText("Real Estate");
		fireEvent.click(screen.getByRole("button", { name: ADD_CATEGORY }));
		const nameInput = screen.getByLabelText(NAME_INPUT_LABEL);

		// THEN
		expect(nameInput.closest("form")).toHaveAttribute("noValidate");
		expect(nameInput).not.toBeRequired();
	});

	it.each(BLANK_NAMES)(
		"should answer $describe with the shared required message and no request",
		async ({ value }) => {
			// GIVEN
			let postedCalls = 0;
			server.use(
				http.post(`${env.apiUrl}/categories`, () => {
					postedCalls += 1;
					return HttpResponse.json(mockCategories[0]);
				})
			);
			renderCategoriesSheet();
			await screen.findByText("Real Estate");
			fireEvent.click(screen.getByRole("button", { name: ADD_CATEGORY }));
			fireEvent.change(screen.getByLabelText(NAME_INPUT_LABEL), { target: { value } });

			// WHEN
			fireEvent.click(screen.getByRole("button", { name: CREATE_CATEGORY }));

			// THEN
			expect(await screen.findByText(MESSAGE.nameRequired)).toBeInTheDocument();
			expect(screen.getAllByText(MESSAGE.nameRequired)).toHaveLength(1);
			expect(postedCalls).toBe(0);
		}
	);

	it("should label a custom asset category as an asset", async () => {
		// GIVEN
		const customAsset = buildCategory({ name: "Stocks" });
		server.use(http.get(`${env.apiUrl}/categories`, () => HttpResponse.json([customAsset])));
		const testSubject = CategoriesSheet;

		// WHEN
		renderWithProviders(createElement(testSubject, { open: true, onOpenChange: () => undefined }));

		// THEN
		expect(await screen.findByText("Asset")).toBeInTheDocument();
	});

	it("should not render the body while the sheet is closed", () => {
		// GIVEN
		useCategoriesFixture();
		const testSubject = CategoriesSheet;

		// WHEN
		renderWithProviders(createElement(testSubject, { open: false, onOpenChange: () => undefined }));

		// THEN
		expect(screen.queryByRole("button", { name: ADD_CATEGORY })).not.toBeInTheDocument();
	});
});
