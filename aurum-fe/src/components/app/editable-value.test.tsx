import { fireEvent, screen } from "@testing-library/react";
import { createElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "@/test/render";
import { usePrivacyStore } from "@/stores/privacy";
import { EditableValue } from "./editable-value";

const CURRENT_VALUE = 100;
const CURRENCY = "EUR";
const LABEL = "amount";
const EDITOR_LABEL = "Edit amount in EUR";
const OPEN_EDITOR = /Edit amount/;
const INVALID_NUMBER_MESSAGE = "Enter a number";
const COMMA_DECIMAL = "1234,56";
const COMMA_DECIMAL_VALUE = 1234.56;

const LETTERS_DRAFT = "abc";
const EMPTY_DRAFT = "";
const UNPARSABLE_DRAFTS = [
	{ name: "letters", draft: LETTERS_DRAFT },
	{ name: "an empty draft", draft: EMPTY_DRAFT }
];

function renderEditor(onSave: (value: number) => void) {
	const testSubject = EditableValue;
	renderWithProviders(
		createElement(testSubject, { value: CURRENT_VALUE, currency: CURRENCY, onSave, label: LABEL })
	);
}

function openEditor() {
	fireEvent.click(screen.getByRole("button", { name: OPEN_EDITOR }));
	return screen.getByLabelText(EDITOR_LABEL);
}

function commitInvalidNumber() {
	const input = openEditor();
	fireEvent.change(input, { target: { value: LETTERS_DRAFT } });
	fireEvent.keyDown(input, { key: "Enter" });
	return input;
}

describe("EditableValue", () => {
	beforeEach(() => {
		usePrivacyStore.setState({ isPrivacy: false });
	});

	it("should save a changed value on blur", () => {
		// GIVEN
		const onSave = vi.fn();
		const testSubject = EditableValue;
		renderWithProviders(
			createElement(testSubject, { value: 100, currency: "EUR", onSave, label: "amount" })
		);

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Edit amount, currently €100.00/ }));
		const input = screen.getByLabelText("Edit amount in EUR");
		fireEvent.change(input, { target: { value: "250.5" } });
		fireEvent.blur(input);

		// THEN
		expect(onSave).toHaveBeenCalledWith(250.5);
	});

	it("should discard the draft on escape", () => {
		// GIVEN
		const onSave = vi.fn();
		const testSubject = EditableValue;
		renderWithProviders(
			createElement(testSubject, { value: 100, currency: "EUR", onSave, label: "amount" })
		);

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Edit amount/ }));
		const input = screen.getByLabelText("Edit amount in EUR");
		fireEvent.change(input, { target: { value: "250" } });
		fireEvent.keyDown(input, { key: "Escape" });

		// THEN
		expect(onSave).not.toHaveBeenCalled();
		expect(screen.getByRole("button", { name: /currently €100.00/ })).toBeInTheDocument();
	});

	it("should save on enter", () => {
		// GIVEN
		const onSave = vi.fn();
		const testSubject = EditableValue;
		renderWithProviders(
			createElement(testSubject, { value: 100, currency: "EUR", onSave, label: "amount" })
		);

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Edit amount/ }));
		const input = screen.getByLabelText("Edit amount in EUR");
		fireEvent.change(input, { target: { value: "150" } });
		fireEvent.keyDown(input, { key: "Enter" });

		// THEN
		expect(onSave).toHaveBeenCalledWith(150);
	});

	it("should not save an unchanged value", () => {
		// GIVEN
		const onSave = vi.fn();
		const testSubject = EditableValue;
		renderWithProviders(
			createElement(testSubject, { value: 100, currency: "EUR", onSave, label: "amount" })
		);

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Edit amount/ }));
		const input = screen.getByLabelText("Edit amount in EUR");
		fireEvent.change(input, { target: { value: "100" } });
		fireEvent.blur(input);

		// THEN
		expect(onSave).not.toHaveBeenCalled();
	});

	it("should align the editor on the left when asked", () => {
		// GIVEN
		const testSubject = EditableValue;
		renderWithProviders(
			createElement(testSubject, {
				value: 100,
				currency: "EUR",
				onSave: vi.fn(),
				label: "amount",
				align: "left"
			})
		);

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Edit amount/ }));

		// THEN
		expect(screen.getByLabelText("Edit amount in EUR")).toHaveClass("text-left");
	});

	it("should be disabled when asked", () => {
		// GIVEN
		const testSubject = EditableValue;

		// WHEN
		renderWithProviders(
			createElement(testSubject, {
				value: 100,
				currency: "EUR",
				onSave: vi.fn(),
				label: "amount",
				disabled: true
			})
		);

		// THEN
		expect(screen.getByRole("button", { name: /Edit amount/ })).toBeDisabled();
	});

	it("should mask the value in privacy mode", async () => {
		// GIVEN
		usePrivacyStore.setState({ isPrivacy: true });
		const testSubject = EditableValue;

		// WHEN
		renderWithProviders(
			createElement(testSubject, { value: 100, currency: "EUR", onSave: vi.fn(), label: "amount" })
		);

		// THEN
		expect(await screen.findByRole("button", { name: /currently ••••••/ })).toBeInTheDocument();
	});

	it("should edit in a decimal text input", () => {
		// GIVEN
		renderEditor(vi.fn());

		// WHEN
		const input = openEditor();

		// THEN
		expect(input).toHaveAttribute("type", "text");
		expect(input).toHaveAttribute("inputMode", "decimal");
		expect(input).not.toHaveAttribute("step");
		expect(input).not.toHaveAttribute("min");
	});

	it("should save a comma decimal as a decimal point value", () => {
		// GIVEN
		const onSave = vi.fn();
		renderEditor(onSave);

		// WHEN
		const input = openEditor();
		fireEvent.change(input, { target: { value: COMMA_DECIMAL } });
		fireEvent.keyDown(input, { key: "Enter" });

		// THEN
		expect(onSave).toHaveBeenCalledWith(COMMA_DECIMAL_VALUE);
	});

	it.each(UNPARSABLE_DRAFTS)(
		"should ask for a number and stay in edit mode when the draft is $name",
		({ draft }) => {
			// GIVEN
			const onSave = vi.fn();
			renderEditor(onSave);

			// WHEN
			const input = openEditor();
			fireEvent.change(input, { target: { value: draft } });
			fireEvent.keyDown(input, { key: "Enter" });

			// THEN
			expect(screen.getByRole("alert").textContent).toBe(INVALID_NUMBER_MESSAGE);
			expect(input).toHaveAttribute("aria-invalid", "true");
			expect(screen.getByLabelText(EDITOR_LABEL)).toBeInTheDocument();
			expect(onSave).not.toHaveBeenCalled();
		}
	);

	it("should ask for a number when an unparsable draft is committed on blur", () => {
		// GIVEN
		const onSave = vi.fn();
		renderEditor(onSave);

		// WHEN
		const input = openEditor();
		fireEvent.change(input, { target: { value: LETTERS_DRAFT } });
		fireEvent.blur(input);

		// THEN
		expect(screen.getByRole("alert").textContent).toBe(INVALID_NUMBER_MESSAGE);
		expect(onSave).not.toHaveBeenCalled();
	});

	it("should clear the message as soon as the draft changes", () => {
		// GIVEN
		renderEditor(vi.fn());
		const input = commitInvalidNumber();
		expect(screen.getByRole("alert")).toBeInTheDocument();

		// WHEN
		fireEvent.change(input, { target: { value: "12" } });

		// THEN
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
		expect(input).toHaveAttribute("aria-invalid", "false");
	});

	it("should cancel and clear the message on escape", () => {
		// GIVEN
		const onSave = vi.fn();
		renderEditor(onSave);
		const input = commitInvalidNumber();
		expect(screen.getByRole("alert")).toBeInTheDocument();

		// WHEN
		fireEvent.keyDown(input, { key: "Escape" });

		// THEN
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
		expect(screen.getByRole("button", { name: /currently €100.00/ })).toBeInTheDocument();
		expect(onSave).not.toHaveBeenCalled();

		// WHEN
		openEditor();

		// THEN
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
	});
});
