import { fireEvent, screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "@/test/render";
import { EditableText } from "./editable-text";

describe("EditableText", () => {
	it("should save the trimmed value on enter", () => {
		// GIVEN
		const onSave = vi.fn();
		const testSubject = EditableText;
		renderWithProviders(createElement(testSubject, { value: "Old name", onSave, label: "name" }));

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Edit name, currently Old name/ }));
		const input = screen.getByLabelText("Edit name");
		fireEvent.change(input, { target: { value: "  New name  " } });
		fireEvent.keyDown(input, { key: "Enter" });

		// THEN
		expect(onSave).toHaveBeenCalledWith("New name");
		expect(screen.getByText("Old name")).toBeInTheDocument();
	});

	it("should discard the draft on escape", () => {
		// GIVEN
		const onSave = vi.fn();
		const testSubject = EditableText;
		renderWithProviders(createElement(testSubject, { value: "Old name", onSave, label: "name" }));

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Edit name/ }));
		fireEvent.change(screen.getByLabelText("Edit name"), { target: { value: "New name" } });
		fireEvent.keyDown(screen.getByLabelText("Edit name"), { key: "Escape" });

		// THEN
		expect(onSave).not.toHaveBeenCalled();
		expect(screen.getByText("Old name")).toBeInTheDocument();
	});

	it("should not save an empty or unchanged value", () => {
		// GIVEN
		const onSave = vi.fn();
		const testSubject = EditableText;
		renderWithProviders(createElement(testSubject, { value: "Old name", onSave, label: "name" }));

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Edit name/ }));
		fireEvent.change(screen.getByLabelText("Edit name"), { target: { value: "   " } });
		fireEvent.blur(screen.getByLabelText("Edit name"));

		// THEN
		expect(onSave).not.toHaveBeenCalled();
	});

	it("should not be editable when disabled", () => {
		// GIVEN
		const testSubject = EditableText;

		// WHEN
		renderWithProviders(
			createElement(testSubject, {
				value: "Locked",
				onSave: vi.fn(),
				label: "name",
				disabled: true
			})
		);

		// THEN
		expect(screen.getByRole("button", { name: /Edit name/ })).toBeDisabled();
	});
});
