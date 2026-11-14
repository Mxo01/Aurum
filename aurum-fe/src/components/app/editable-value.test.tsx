import { fireEvent, screen } from "@testing-library/react";
import { createElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "@/test/render";
import { usePrivacyStore } from "@/stores/privacy";
import { EditableValue } from "./editable-value";

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
});
