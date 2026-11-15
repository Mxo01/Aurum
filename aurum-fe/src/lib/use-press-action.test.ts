import { fireEvent, render, screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { PRESS_MOVEMENT_TOLERANCE_PX, usePressAction } from "./use-press-action";

const PRESS_TARGET_LABEL = "Press target";
const PRIMARY_POINTER_ID = 7;
const SECOND_POINTER_ID = 9;
const PRIMARY_BUTTON = 0;
const SECONDARY_BUTTON = 2;
const KEYBOARD_CLICK_DETAIL = 0;
const POINTER_CLICK_DETAIL = 1;
const FIRED_ONCE = 1;
const NEVER_FIRED = 0;
const ORIGIN_X = 120;
const ORIGIN_Y = 240;
const WITHIN_TOLERANCE_DIAGONAL = 6;
const BEYOND_TOLERANCE_DIAGONAL = 8;

interface PressOptions {
	pointerId?: number;
	clientX?: number;
	clientY?: number;
	isPrimary?: boolean;
	button?: number;
}

function PressTarget({ action }: { readonly action: () => void }) {
	return createElement("button", { type: "button", ...usePressAction(action) }, PRESS_TARGET_LABEL);
}

function renderPressAction() {
	// GIVEN
	const action = vi.fn();
	render(createElement(PressTarget, { action }));
	const testSubject = screen.getByRole("button", { name: PRESS_TARGET_LABEL });
	return { action, testSubject };
}

function pointerDown(target: HTMLElement, options: PressOptions = {}) {
	fireEvent.pointerDown(target, {
		pointerId: PRIMARY_POINTER_ID,
		isPrimary: true,
		button: PRIMARY_BUTTON,
		clientX: ORIGIN_X,
		clientY: ORIGIN_Y,
		...options
	});
}

function pointerUp(target: HTMLElement, options: PressOptions = {}) {
	fireEvent.pointerUp(target, {
		pointerId: PRIMARY_POINTER_ID,
		isPrimary: true,
		button: PRIMARY_BUTTON,
		clientX: ORIGIN_X,
		clientY: ORIGIN_Y,
		...options
	});
}

const MOVEMENT_CASES = [
	{ name: "a press that never moves", dx: 0, dy: 0, expectedCalls: FIRED_ONCE },
	{
		name: "travel exactly at the tolerance",
		dx: PRESS_MOVEMENT_TOLERANCE_PX,
		dy: 0,
		expectedCalls: FIRED_ONCE
	},
	{
		name: "travel one pixel past the tolerance",
		dx: PRESS_MOVEMENT_TOLERANCE_PX + 1,
		dy: 0,
		expectedCalls: NEVER_FIRED
	},
	{
		name: "diagonal travel inside the tolerance",
		dx: WITHIN_TOLERANCE_DIAGONAL,
		dy: WITHIN_TOLERANCE_DIAGONAL,
		expectedCalls: FIRED_ONCE
	},
	{
		name: "diagonal travel outside the tolerance",
		dx: BEYOND_TOLERANCE_DIAGONAL,
		dy: BEYOND_TOLERANCE_DIAGONAL,
		expectedCalls: NEVER_FIRED
	}
];

const NON_ARMING_CASES = [
	{ name: "a non-primary pointer", pointerDownOptions: { isPrimary: false } },
	{ name: "a non-primary button", pointerDownOptions: { button: SECONDARY_BUTTON } }
];

describe("usePressAction", () => {
	describe("on the pointer gesture", () => {
		it.each(MOVEMENT_CASES)("should run the action once on $name", ({ dx, dy, expectedCalls }) => {
			// GIVEN
			const { action, testSubject } = renderPressAction();
			pointerDown(testSubject);

			// WHEN
			pointerUp(testSubject, { clientX: ORIGIN_X + dx, clientY: ORIGIN_Y + dy });

			// THEN
			expect(action).toHaveBeenCalledTimes(expectedCalls);
		});

		it.each(NON_ARMING_CASES)("should not arm on $name", ({ pointerDownOptions }) => {
			// GIVEN
			const { action, testSubject } = renderPressAction();

			// WHEN
			pointerDown(testSubject, pointerDownOptions);
			pointerUp(testSubject);

			// THEN
			expect(action).not.toHaveBeenCalled();
		});

		it("should discard the gesture on pointercancel", () => {
			// GIVEN
			const { action, testSubject } = renderPressAction();
			pointerDown(testSubject);

			// WHEN
			fireEvent.pointerCancel(testSubject, { pointerId: PRIMARY_POINTER_ID });
			pointerUp(testSubject);

			// THEN
			expect(action).not.toHaveBeenCalled();
		});

		it("should discard the gesture when a second pointer lands", () => {
			// GIVEN
			const { action, testSubject } = renderPressAction();
			pointerDown(testSubject);

			// WHEN
			pointerDown(testSubject, { pointerId: SECOND_POINTER_ID });
			pointerUp(testSubject);
			pointerUp(testSubject, { pointerId: SECOND_POINTER_ID });

			// THEN
			expect(action).not.toHaveBeenCalled();
		});

		it("should do nothing on a pointerup without an armed press", () => {
			// GIVEN
			const { action, testSubject } = renderPressAction();

			// WHEN
			pointerUp(testSubject);

			// THEN
			expect(action).not.toHaveBeenCalled();
		});

		it("should do nothing on a pointerup from a different pointer", () => {
			// GIVEN
			const { action, testSubject } = renderPressAction();
			pointerDown(testSubject);

			// WHEN
			pointerUp(testSubject, { pointerId: SECOND_POINTER_ID });

			// THEN
			expect(action).not.toHaveBeenCalled();
		});
	});

	describe("on the click", () => {
		it("should run the action for a keyboard click", () => {
			// GIVEN
			const { action, testSubject } = renderPressAction();

			// WHEN
			fireEvent.click(testSubject, { detail: KEYBOARD_CLICK_DETAIL });

			// THEN
			expect(action).toHaveBeenCalledTimes(FIRED_ONCE);
		});

		it("should ignore a click that carries a pointer detail", () => {
			// GIVEN
			const { action, testSubject } = renderPressAction();

			// WHEN
			fireEvent.click(testSubject, { detail: POINTER_CLICK_DETAIL });

			// THEN
			expect(action).not.toHaveBeenCalled();
		});

		it("should not run the action twice for a gesture and the click that follows", () => {
			// GIVEN
			const { action, testSubject } = renderPressAction();
			pointerDown(testSubject);

			// WHEN
			pointerUp(testSubject);
			fireEvent.click(testSubject, { detail: POINTER_CLICK_DETAIL });

			// THEN
			expect(action).toHaveBeenCalledTimes(FIRED_ONCE);
		});
	});
});
