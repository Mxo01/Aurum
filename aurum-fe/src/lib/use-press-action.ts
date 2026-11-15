import { useCallback, useMemo, useRef } from "react";
import type { MouseEvent, PointerEvent } from "react";

/**
 * How far a pointer may travel between down and up and still count as a press.
 * Wider travel is a drag (scrolling a sheet, swiping) and must not activate.
 */
export const PRESS_MOVEMENT_TOLERANCE_PX = 10;

interface PressOrigin {
	pointerId: number;
	x: number;
	y: number;
}

export interface PressActionHandlers {
	onPointerDown: (event: PointerEvent<HTMLElement>) => void;
	onPointerUp: (event: PointerEvent<HTMLElement>) => void;
	onPointerCancel: () => void;
	onClick: (event: MouseEvent<HTMLElement>) => void;
}

/**
 * Runs an action on the pointer gesture itself instead of on `click`, because
 * WebKit inside a dismissing bottom sheet can deliver the click to whatever is
 * underneath the finger. A press that ends where it started always wins.
 */
export function usePressAction(action: () => void): PressActionHandlers {
	const origin = useRef<PressOrigin | null>(null);

	const onPointerDown = useCallback((event: PointerEvent<HTMLElement>) => {
		const armed = origin.current;
		if (armed && armed.pointerId !== event.pointerId) {
			// A second finger means a scroll or a pinch: drop the gesture rather
			// than firing on whichever pointer happens to lift first.
			origin.current = null;
			return;
		}
		if (event.isPrimary === false || event.button !== 0) return;
		origin.current = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
	}, []);

	const onPointerUp = useCallback(
		(event: PointerEvent<HTMLElement>) => {
			const armed = origin.current;
			if (!armed || armed.pointerId !== event.pointerId) return;
			origin.current = null;
			const travelled = Math.hypot(event.clientX - armed.x, event.clientY - armed.y);
			if (travelled <= PRESS_MOVEMENT_TOLERANCE_PX) action();
		},
		[action]
	);

	const onPointerCancel = useCallback(() => {
		origin.current = null;
	}, []);

	const onClick = useCallback(
		(event: MouseEvent<HTMLElement>) => {
			// A pointer tap already ran the action on pointerup and its click
			// carries detail > 0, so only keyboard activation reaches it here.
			if (event.detail === 0) action();
		},
		[action]
	);

	return useMemo(
		() => ({ onPointerDown, onPointerUp, onPointerCancel, onClick }),
		[onPointerDown, onPointerUp, onPointerCancel, onClick]
	);
}
