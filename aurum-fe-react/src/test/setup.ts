import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

class MemoryStorage implements Storage {
	private readonly items = new Map<string, string>();

	get length() {
		return this.items.size;
	}

	clear() {
		this.items.clear();
	}

	getItem(key: string) {
		return this.items.get(key) ?? null;
	}

	key(index: number) {
		return Array.from(this.items.keys())[index] ?? null;
	}

	removeItem(key: string) {
		this.items.delete(key);
	}

	setItem(key: string, value: string) {
		this.items.set(key, value);
	}
}

// Node 26 exposes an undefined `localStorage` global without `--localstorage-file`,
// which shadows the DOM environment's implementation. Restore a real one for tests.
if (!window.localStorage) {
	const storage = new MemoryStorage();
	Object.defineProperty(window, "localStorage", { value: storage, configurable: true });
	Object.defineProperty(globalThis, "localStorage", { value: storage, configurable: true });
}

afterEach(() => {
	cleanup();
});
