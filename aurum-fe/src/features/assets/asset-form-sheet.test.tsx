import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { addMonths, format, startOfMonth } from "date-fns";
import { enUS } from "date-fns/locale";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { env } from "@/lib/env";
import type { ApiErrorBody, ApiErrorDetail, AssetDTO } from "@/lib/api/types";
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
		expect(updatedBody).toMatchObject({
			name: "Apartment Berlin",
			categoryId: "c1",
			liabilityType: null
		});
	});

	it("should keep the liability block when editing an automatic liability", async () => {
		// GIVEN
		let updatedBody: unknown;
		server.use(
			http.put(`${env.apiUrl}/assets/:id`, async ({ request }) => {
				updatedBody = await request.json();
				return HttpResponse.json(buildAsset());
			})
		);
		const mockLiability = buildAsset({
			id: "a2",
			name: "Mortgage",
			categoryId: "c2",
			categoryName: "Mortgage",
			type: "LIABILITY",
			liabilityType: "AUTOMATIC",
			paymentFrequency: "MONTHLY",
			paymentAmount: 500
		});
		const testSubject = AssetFormSheet;
		const Subject = testSubject;
		renderWithProviders(
			<Subject
				open
				onOpenChange={() => undefined}
				asset={mockLiability}
				categories={mockCategories}
			/>
		);

		// WHEN
		await waitFor(() => expect(screen.getByRole("button", { name: "Save changes" })).toBeEnabled());
		fireEvent.submit(
			screen.getByRole("button", { name: "Save changes" }).closest("form") as HTMLFormElement
		);

		// THEN
		await waitFor(() => expect(updatedBody).toBeDefined());
		expect(updatedBody).toMatchObject({
			categoryId: "c2",
			liabilityType: "AUTOMATIC",
			paymentFrequency: "MONTHLY",
			paymentAmount: 500
		});
	});

	it("should not clear the liability block when creating an asset", async () => {
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
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Apartment" } });
		await user.click(screen.getByLabelText("Category"));
		await user.click(await screen.findByRole("option", { name: "Investments" }));
		await waitFor(() => expect(screen.getByRole("button", { name: "Add asset" })).toBeEnabled());
		fireEvent.submit(
			screen.getByRole("button", { name: "Add asset" }).closest("form") as HTMLFormElement
		);

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).not.toHaveProperty("liabilityType");
		expect(postedBody).not.toHaveProperty("paymentFrequency");
		expect(postedBody).not.toHaveProperty("paymentAmount");
	});

	it("should render nothing while the sheet is closed", () => {
		// GIVEN
		const testSubject = AssetFormSheet;
		const Subject = testSubject;

		// WHEN
		renderWithProviders(
			<Subject
				open={false}
				onOpenChange={() => undefined}
				asset={null}
				categories={mockCategories}
			/>
		);

		// THEN
		expect(screen.queryByRole("heading", { name: "New asset" })).not.toBeInTheDocument();
	});
});

describe("AssetFormSheet category groups", () => {
	const cases = [
		{
			description: "only asset categories",
			categories: [buildCategory({ id: "c1", name: "Investments", type: "ASSET" })],
			categoryName: "Investments",
			shownGroup: "Assets",
			hiddenGroup: "Liabilities"
		},
		{
			description: "only liability categories",
			categories: [buildCategory({ id: "c2", name: "Mortgage", type: "LIABILITY" })],
			categoryName: "Mortgage",
			shownGroup: "Liabilities",
			hiddenGroup: "Assets"
		}
	];

	it.each(cases)(
		"should list $shownGroup and drop $hiddenGroup with $description",
		async ({ categories, categoryName, shownGroup, hiddenGroup }) => {
			// GIVEN
			const testSubject = AssetFormSheet;
			const Subject = testSubject;
			renderWithProviders(
				<Subject
					open
					onOpenChange={() => undefined}
					asset={null}
					categories={categories}
				/>
			);

			// WHEN
			await userEvent.click(screen.getByLabelText("Category"));
			await screen.findByRole("option", { name: categoryName });

			// THEN
			expect(screen.getByText(shownGroup)).toBeInTheDocument();
			expect(screen.queryByText(hiddenGroup)).not.toBeInTheDocument();
		}
	);
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

// The envelope the backend answers a rejected write with: the sheet reads `errors` and places each
// detail on the input it names, keeping the rest for the alert.
const REJECTION_STATUS = 400;
const REJECTION_ERROR = "Bad Request";
const ASSET_CATEGORY_NAME = "Investments";
const LIABILITY_CATEGORY_NAME = "Mortgage";
const SUBMITTED_NAME = "Apartment Berlin";
const SUBMITTED_PAYMENT_AMOUNT = "500";
const AUTOMATIC = "Automatic";
const MONTHLY = "Monthly";

interface ServerDetail {
	field?: string;
	message: string;
}

const rejectionDetails = {
	name: { field: "name", message: "Name is already taken" },
	categoryId: { field: "categoryId", message: "Category is no longer available" },
	liabilityType: { field: "liabilityType", message: "Liability type is not allowed here" },
	paymentFrequency: { field: "paymentFrequency", message: "Frequency is not supported" },
	paymentAmount: { field: "paymentAmount", message: "Payment exceeds the outstanding balance" },
	initialValue: { field: "initialValue", message: "Initial value is not allowed" },
	referenceDate: { field: "referenceDate", message: "The value date cannot be in the future" },
	unfielded: { message: "The request was rejected" }
} satisfies Record<string, ServerDetail>;

const CREATE_FALLBACK = "Could not create the asset";
const UPDATE_FALLBACK = "Could not update the asset";

function rejectionBody(details: readonly ServerDetail[]): ApiErrorBody {
	return {
		status: REJECTION_STATUS,
		error: REJECTION_ERROR,
		errors: details.map(({ field, message }): ApiErrorDetail =>
			field ? { field, message } : { message }
		)
	};
}

function rejectCreate(details: readonly ServerDetail[]) {
	server.use(
		http.post(`${env.apiUrl}/assets`, () =>
			HttpResponse.json(rejectionBody(details), { status: REJECTION_STATUS })
		)
	);
}

function rejectUpdate(details: readonly ServerDetail[]) {
	server.use(
		http.put(`${env.apiUrl}/assets/:id`, () =>
			HttpResponse.json(rejectionBody(details), { status: REJECTION_STATUS })
		)
	);
}

function failCreateWithoutEnvelope() {
	server.use(http.post(`${env.apiUrl}/assets`, () => HttpResponse.error()));
}

function failUpdateWithoutEnvelope() {
	server.use(http.put(`${env.apiUrl}/assets/:id`, () => HttpResponse.error()));
}

const noopOpenChange = () => undefined;

function sheet(open: boolean, asset: AssetDTO | null) {
	const testSubject = AssetFormSheet;
	const Subject = testSubject;

	return (
		<Subject
			open={open}
			onOpenChange={noopOpenChange}
			asset={asset}
			categories={mockCategories}
		/>
	);
}

async function selectOption(label: string, optionName: string) {
	const user = userEvent.setup();
	await user.click(screen.getByLabelText(label));
	await user.click(await screen.findByRole("option", { name: optionName }));
}

interface CreateFormFill {
	categoryName: string;
	automatic?: boolean;
}

async function fillCreateForm({ categoryName, automatic = false }: CreateFormFill) {
	fireEvent.change(screen.getByLabelText("Name"), { target: { value: SUBMITTED_NAME } });
	await selectOption("Category", categoryName);
	if (!automatic) return;

	await selectOption("Liability type", AUTOMATIC);
	await selectOption("Payment frequency", MONTHLY);
	fireEvent.change(screen.getByLabelText("Payment amount"), {
		target: { value: SUBMITTED_PAYMENT_AMOUNT }
	});
}

function submitButton(name: string) {
	const button = screen.getByRole("button", { name });
	const form = button.closest("form");
	if (!form) throw new Error(`No form around the ${name} button`);
	return { button, form };
}

async function submit(buttonName: string) {
	const { button, form } = submitButton(buttonName);
	await waitFor(() => expect(button).toBeEnabled());
	fireEvent.submit(form);
}

function fieldGroup(label: string): HTMLElement {
	const labelElement = screen.getByText(label);
	const group = labelElement.closest<HTMLElement>("div");
	if (!group) throw new Error(`No field group around the ${label} label`);
	return group;
}

function isBefore(first: Node, second: Node) {
	return Boolean(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING);
}

const mockEditableAsset = buildAsset({
	id: "a1",
	name: "Apartment",
	categoryId: "c1",
	categoryName: ASSET_CATEGORY_NAME,
	categoryIcon: null,
	type: "ASSET",
	originalCurrency: "EUR"
});

describe("AssetFormSheet rejection", () => {
	const placedCases: {
		description: string;
		detail: ServerDetail;
		fieldLabel: string;
		categoryName: string;
		automatic?: boolean;
	}[] = [
		{
			description: "the name",
			detail: rejectionDetails.name,
			fieldLabel: "Name",
			categoryName: ASSET_CATEGORY_NAME
		},
		{
			description: "the category",
			detail: rejectionDetails.categoryId,
			fieldLabel: "Category",
			categoryName: ASSET_CATEGORY_NAME
		},
		{
			description: "the liability type",
			detail: rejectionDetails.liabilityType,
			fieldLabel: "Liability type",
			categoryName: LIABILITY_CATEGORY_NAME
		},
		{
			description: "the payment frequency",
			detail: rejectionDetails.paymentFrequency,
			fieldLabel: "Payment frequency",
			categoryName: LIABILITY_CATEGORY_NAME,
			automatic: true
		},
		{
			description: "the payment amount",
			detail: rejectionDetails.paymentAmount,
			fieldLabel: "Payment amount",
			categoryName: LIABILITY_CATEGORY_NAME,
			automatic: true
		},
		{
			description: "the initial value",
			detail: rejectionDetails.initialValue,
			fieldLabel: "Initial value",
			categoryName: ASSET_CATEGORY_NAME
		},
		{
			description: "the value date",
			detail: rejectionDetails.referenceDate,
			fieldLabel: "Value date",
			categoryName: ASSET_CATEGORY_NAME
		}
	];

	it.each(placedCases)(
		"should place the rejection on $description and nowhere else",
		async ({ detail, fieldLabel, categoryName, automatic }) => {
			// GIVEN
			rejectCreate([detail]);
			renderWithProviders(sheet(true, null));
			await fillCreateForm({ categoryName, automatic });

			// WHEN
			await submit("Add asset");

			// THEN
			await waitFor(() =>
				expect(within(fieldGroup(fieldLabel)).getByText(detail.message)).toBeInTheDocument()
			);
			expect(screen.getAllByText(detail.message)).toHaveLength(1);
			expect(screen.queryByRole("alert")).not.toBeInTheDocument();
			expect(screen.getByRole("button", { name: "Add asset" })).toBeEnabled();
		}
	);

	it("should replace the client message of the same field with the server one", async () => {
		// GIVEN
		rejectCreate([rejectionDetails.name]);
		renderWithProviders(sheet(true, null));
		await fillCreateForm({ categoryName: ASSET_CATEGORY_NAME });

		// WHEN
		await submit("Add asset");

		// THEN
		await waitFor(() =>
			expect(
				within(fieldGroup("Name")).getByText(rejectionDetails.name.message)
			).toBeInTheDocument()
		);
		expect(within(fieldGroup("Name")).queryByText("Name is required")).not.toBeInTheDocument();
	});

	it("should collect every detail the sheet cannot place in one alert above the footer", async () => {
		// GIVEN
		rejectUpdate([rejectionDetails.unfielded, rejectionDetails.referenceDate]);
		renderWithProviders(sheet(true, mockEditableAsset));

		// WHEN
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: SUBMITTED_NAME } });
		await submit("Save changes");

		// THEN
		await waitFor(() => expect(screen.getByRole("alert")).toBeInTheDocument());
		const alert = screen.getByRole("alert");
		expect(screen.getAllByRole("alert")).toHaveLength(1);
		const lines = within(alert)
			.getAllByRole("paragraph")
			.map(line => line.textContent);
		expect(lines).toEqual([
			rejectionDetails.unfielded.message,
			rejectionDetails.referenceDate.message
		]);
		expect(screen.getAllByText(rejectionDetails.unfielded.message)).toHaveLength(1);
		expect(screen.getAllByText(rejectionDetails.referenceDate.message)).toHaveLength(1);
		expect(isBefore(alert, submitButton("Save changes").button)).toBe(true);
	});

	it("should show the create fallback when the failure carries no envelope", async () => {
		// GIVEN
		failCreateWithoutEnvelope();
		renderWithProviders(sheet(true, null));
		await fillCreateForm({ categoryName: ASSET_CATEGORY_NAME });

		// WHEN
		await submit("Add asset");

		// THEN
		await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent(CREATE_FALLBACK));
		expect(screen.getAllByRole("alert")).toHaveLength(1);
		expect(isBefore(screen.getByRole("alert"), submitButton("Add asset").button)).toBe(true);
	});

	it("should show the update fallback when the failure carries no envelope", async () => {
		// GIVEN
		failUpdateWithoutEnvelope();
		renderWithProviders(sheet(true, mockEditableAsset));

		// WHEN
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: SUBMITTED_NAME } });
		await submit("Save changes");

		// THEN
		await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent(UPDATE_FALLBACK));
		expect(screen.getAllByRole("alert")).toHaveLength(1);
	});

	it("should leave the submit button enabled and accept a second submit without an edit", async () => {
		// GIVEN
		const requests: string[] = [];
		server.use(
			http.post(`${env.apiUrl}/assets`, ({ request }) => {
				requests.push(request.method);
				return HttpResponse.json(rejectionBody([rejectionDetails.name]), {
					status: REJECTION_STATUS
				});
			})
		);
		renderWithProviders(sheet(true, null));
		await fillCreateForm({ categoryName: ASSET_CATEGORY_NAME });
		await submit("Add asset");
		await waitFor(() =>
			expect(
				within(fieldGroup("Name")).getByText(rejectionDetails.name.message)
			).toBeInTheDocument()
		);

		// WHEN
		await submit("Add asset");

		// THEN
		await waitFor(() => expect(requests).toHaveLength(2));
		expect(submitButton("Add asset").button).toBeEnabled();
	});

	it("should clear every rejection line when a value is edited", async () => {
		// GIVEN
		rejectCreate([rejectionDetails.name, rejectionDetails.unfielded]);
		renderWithProviders(sheet(true, null));
		await fillCreateForm({ categoryName: ASSET_CATEGORY_NAME });
		await submit("Add asset");
		await waitFor(() =>
			expect(screen.getByRole("alert")).toHaveTextContent(rejectionDetails.unfielded.message)
		);
		expect(within(fieldGroup("Name")).getByText(rejectionDetails.name.message)).toBeInTheDocument();

		// WHEN
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: `${SUBMITTED_NAME} 2` } });

		// THEN
		await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
		expect(screen.queryByText(rejectionDetails.name.message)).not.toBeInTheDocument();
		expect(screen.queryByText(rejectionDetails.unfielded.message)).not.toBeInTheDocument();
	});

	it("should clear every rejection line when the sheet is reopened", async () => {
		// GIVEN
		rejectCreate([rejectionDetails.name, rejectionDetails.unfielded]);
		const view = renderWithProviders(sheet(true, null));
		await fillCreateForm({ categoryName: ASSET_CATEGORY_NAME });
		await submit("Add asset");
		await waitFor(() =>
			expect(screen.getByRole("alert")).toHaveTextContent(rejectionDetails.unfielded.message)
		);
		expect(within(fieldGroup("Name")).getByText(rejectionDetails.name.message)).toBeInTheDocument();

		// WHEN
		view.rerender(sheet(false, null));
		view.rerender(sheet(true, null));

		// THEN
		await waitFor(() =>
			expect(screen.getByRole("heading", { name: "New asset" })).toBeInTheDocument()
		);
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
		expect(screen.queryByText(rejectionDetails.name.message)).not.toBeInTheDocument();
		expect(screen.queryByText(rejectionDetails.unfielded.message)).not.toBeInTheDocument();
	});
});

// The sentences #91 puts on the screen: short, human, and never the zod default.
const AMOUNT_REQUIRED = "Enter an amount";
const NUMBER_INVALID = "Enter a number";
const AMOUNT_NOT_NEGATIVE = "Cannot be negative";
const PAYMENT_AMOUNT_REQUIRED = "Enter the payment amount";
const DATE_NOT_FUTURE = "The date cannot be in the future";
const ZOD_DEFAULT_MESSAGE = /Invalid input/;
const NO_BROWSER_VALIDATION = "novalidate";

const COMMA_INITIAL_VALUE = "1234,56";
const COMMA_INITIAL_VALUE_NUMBER = 1234.56;

function expectBrowserSafeAmountField(field: HTMLElement) {
	expect(field).toHaveAttribute("type", "text");
	expect(field).toHaveAttribute("inputmode", "decimal");
	expect(field).not.toHaveAttribute("min");
	expect(field).not.toHaveAttribute("step");
}

describe("AssetFormSheet amount messages", () => {
	const initialValueCases = [
		{ description: "an empty initial value", value: "", message: AMOUNT_REQUIRED },
		{ description: "an unparsable initial value", value: "abc", message: NUMBER_INVALID },
		{ description: "a negative initial value", value: "-10", message: AMOUNT_NOT_NEGATIVE }
	];

	it.each(initialValueCases)(
		'should answer $description with "$message" and never the zod default',
		async ({ value, message }) => {
			// GIVEN
			renderWithProviders(sheet(true, null));

			// WHEN
			fireEvent.change(screen.getByLabelText("Initial value"), { target: { value } });

			// THEN
			expect(await within(fieldGroup("Initial value")).findByText(message)).toBeInTheDocument();
			expect(screen.getAllByText(message)).toHaveLength(1);
			expect(screen.queryByText(ZOD_DEFAULT_MESSAGE)).not.toBeInTheDocument();
		}
	);

	const paymentCases = [
		{ description: "an empty payment", value: "", message: PAYMENT_AMOUNT_REQUIRED },
		{ description: "a zero payment", value: "0", message: PAYMENT_AMOUNT_REQUIRED },
		{ description: "an unparsable payment", value: "abc", message: NUMBER_INVALID }
	];

	it.each(paymentCases)(
		'should answer $description of an automatic liability with "$message"',
		async ({ value, message }) => {
			// GIVEN
			renderWithProviders(sheet(true, null));
			await fillCreateForm({ categoryName: LIABILITY_CATEGORY_NAME, automatic: true });

			// WHEN
			fireEvent.change(screen.getByLabelText("Payment amount"), { target: { value } });

			// THEN
			expect(await within(fieldGroup("Payment amount")).findByText(message)).toBeInTheDocument();
			expect(screen.getAllByText(message)).toHaveLength(1);
			expect(screen.queryByText(ZOD_DEFAULT_MESSAGE)).not.toBeInTheDocument();
		}
	);

	it("should say a negative payment once", async () => {
		// GIVEN
		renderWithProviders(sheet(true, null));
		await fillCreateForm({ categoryName: LIABILITY_CATEGORY_NAME, automatic: true });

		// WHEN
		fireEvent.change(screen.getByLabelText("Payment amount"), { target: { value: "-5" } });

		// THEN
		expect(
			await within(fieldGroup("Payment amount")).findByText(AMOUNT_NOT_NEGATIVE)
		).toBeInTheDocument();
		expect(screen.getAllByText(AMOUNT_NOT_NEGATIVE)).toHaveLength(1);
		expect(screen.queryByText(PAYMENT_AMOUNT_REQUIRED)).not.toBeInTheDocument();
	});
});

describe("AssetFormSheet browser validation", () => {
	it("should leave every amount to our own validation", async () => {
		// GIVEN
		renderWithProviders(sheet(true, null));
		await selectOption("Category", LIABILITY_CATEGORY_NAME);
		await selectOption("Liability type", AUTOMATIC);

		// WHEN
		const { form } = submitButton("Add asset");

		// THEN
		expect(form).toHaveAttribute(NO_BROWSER_VALIDATION);
		expectBrowserSafeAmountField(screen.getByLabelText("Initial value"));
		expectBrowserSafeAmountField(screen.getByLabelText("Payment amount"));
	});
});

function sentInitialValue(body: unknown): unknown {
	if (typeof body !== "object" || body === null || !("initialValue" in body)) {
		throw new Error("The create request carried no initial value");
	}

	return body.initialValue;
}

describe("AssetFormSheet comma decimals", () => {
	it("should send a comma amount to the API as a number", async () => {
		// GIVEN
		let postedBody: unknown;
		server.use(
			http.post(`${env.apiUrl}/assets`, async ({ request }) => {
				postedBody = await request.json();
				return HttpResponse.json(buildAsset());
			})
		);
		renderWithProviders(sheet(true, null));
		await fillCreateForm({ categoryName: ASSET_CATEGORY_NAME });

		// WHEN
		fireEvent.change(screen.getByLabelText("Initial value"), {
			target: { value: COMMA_INITIAL_VALUE }
		});
		await submit("Add asset");

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).toMatchObject({ initialValue: COMMA_INITIAL_VALUE_NUMBER });
		expect(typeof sentInitialValue(postedBody)).toBe("number");
		expect(screen.queryByText(ZOD_DEFAULT_MESSAGE)).not.toBeInTheDocument();
	});
});

const FROZEN_TODAY = new Date(2026, 2, 15, 12);

function calendarDayLabel(date: Date) {
	return format(date, "PPPP", { locale: enUS });
}

const NEXT_MONTH_FIRST_DAY_LABEL = calendarDayLabel(startOfMonth(addMonths(FROZEN_TODAY, 1)));
const TODAY_DAY_LABEL = `Today, ${calendarDayLabel(FROZEN_TODAY)}`;

describe("AssetFormSheet value date", () => {
	afterEach(() => {
		vi.useRealTimers();
	});

	it("should reject a future date picked from the calendar and accept today again", async () => {
		// GIVEN
		vi.useFakeTimers({ toFake: ["Date"] });
		vi.setSystemTime(FROZEN_TODAY);
		const user = userEvent.setup();
		renderWithProviders(sheet(true, null));

		// WHEN
		await user.click(screen.getByRole("button", { name: "Value date" }));
		await user.click(await screen.findByRole("button", { name: "Go to the Next Month" }));
		await user.click(screen.getByRole("button", { name: NEXT_MONTH_FIRST_DAY_LABEL }));

		// THEN
		expect(await within(fieldGroup("Value date")).findByText(DATE_NOT_FUTURE)).toBeInTheDocument();
		expect(screen.getAllByText(DATE_NOT_FUTURE)).toHaveLength(1);
		expect(screen.queryByText(ZOD_DEFAULT_MESSAGE)).not.toBeInTheDocument();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Value date" }));
		await user.click(await screen.findByRole("button", { name: "Go to the Previous Month" }));
		await user.click(screen.getByRole("button", { name: TODAY_DAY_LABEL }));

		// THEN
		await waitFor(() => expect(screen.queryByText(DATE_NOT_FUTURE)).not.toBeInTheDocument());
	});
});
