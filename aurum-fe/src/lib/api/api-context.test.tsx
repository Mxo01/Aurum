import { render, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiProvider, useApi } from "./api-context";
import type { ApiClient } from "./client";

interface ProbeProps {
	onClient: (client: ApiClient) => void;
}

function Probe({ onClient }: ProbeProps) {
	onClient(useApi());
	return null;
}

interface HarnessProps extends ProbeProps {
	getToken?: () => Promise<string | undefined>;
}

function Harness({ getToken, onClient }: HarnessProps) {
	return (
		<ApiProvider getToken={getToken}>
			<Probe onClient={onClient} />
		</ApiProvider>
	);
}

function jsonResponse() {
	return new Response(JSON.stringify({ id: "1" }), {
		status: 200,
		headers: { "content-type": "application/json" }
	});
}

describe("ApiProvider", () => {
	afterEach(() => {
		vi.restoreAllMocks();
	});

	it("should provide a client that sends the token from getToken", async () => {
		// GIVEN
		const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse());
		const token = "token-123";
		let client: ApiClient | undefined;
		render(
			<Harness
				getToken={async () => token}
				onClient={next => (client = next)}
			/>
		);

		// WHEN
		await client?.GET("/users");

		// THEN
		const request = fetchSpy.mock.calls[0]?.[0] as Request;
		expect(request.headers.get("Authorization")).toBe(`Bearer ${token}`);
	});

	it("should provide a client without a token when getToken is omitted", async () => {
		// GIVEN
		const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse());
		let client: ApiClient | undefined;
		render(<Harness onClient={next => (client = next)} />);

		// WHEN
		await client?.GET("/users");

		// THEN
		const request = fetchSpy.mock.calls[0]?.[0] as Request;
		expect(request.headers.get("Authorization")).toBeNull();
	});

	it("should keep the same client while getToken is stable", () => {
		// GIVEN
		const getToken = async () => "token";
		const clients: ApiClient[] = [];
		const { rerender } = render(
			<Harness
				getToken={getToken}
				onClient={next => clients.push(next)}
			/>
		);

		// WHEN
		rerender(
			<Harness
				getToken={getToken}
				onClient={next => clients.push(next)}
			/>
		);

		// THEN
		expect(clients).toHaveLength(2);
		expect(clients[0]).toBe(clients[1]);
	});

	it("should rebuild the client when getToken changes", () => {
		// GIVEN
		const firstToken = async () => "first";
		const secondToken = async () => "second";
		const clients: ApiClient[] = [];
		const { rerender } = render(
			<Harness
				getToken={firstToken}
				onClient={next => clients.push(next)}
			/>
		);

		// WHEN
		rerender(
			<Harness
				getToken={secondToken}
				onClient={next => clients.push(next)}
			/>
		);

		// THEN
		expect(clients).toHaveLength(2);
		expect(clients[0]).not.toBe(clients[1]);
	});
});

describe("useApi", () => {
	afterEach(() => {
		vi.restoreAllMocks();
	});

	it("should return the client of the enclosing provider", () => {
		// GIVEN
		const getToken = async () => undefined;

		// WHEN
		const { result } = renderHook(() => useApi(), {
			wrapper: ({ children }) => <ApiProvider getToken={getToken}>{children}</ApiProvider>
		});

		// THEN
		expect(result.current).toBeDefined();
		expect(typeof result.current.GET).toBe("function");
	});

	it("should throw outside of an ApiProvider", () => {
		// GIVEN
		const testSubject = () => useApi();
		vi.spyOn(console, "error").mockImplementation(() => undefined);

		// WHEN
		const call = () => renderHook(testSubject);

		// THEN
		expect(call).toThrow("useApi must be used inside ApiProvider");
	});
});
