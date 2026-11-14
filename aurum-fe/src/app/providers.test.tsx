import { render, screen } from "@testing-library/react";
import type * as apiClientTypes from "@/lib/api/client";
import type * as apiContextTypes from "@/lib/api/api-context";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mockUseAuth0 = vi.fn();
const mockRegister = vi.fn();
let capturedRedirect: ((state?: { returnTo?: string }) => void) | undefined;

vi.mock("@auth0/auth0-react", () => ({
	Auth0Provider: ({
		children,
		onRedirectCallback
	}: {
		children: ReactNode;
		onRedirectCallback: (state?: { returnTo?: string }) => void;
	}) => {
		capturedRedirect = onRedirectCallback;
		return children;
	},
	useAuth0: () => mockUseAuth0()
}));

vi.mock("@/lib/api/api-context", async importOriginal => {
	const actual = await importOriginal<typeof apiContextTypes>();
	return {
		...actual,
		ApiProvider: ({
			children,
			getToken
		}: {
			children: ReactNode;
			getToken?: () => Promise<string>;
		}) => {
			void getToken?.();
			return children;
		}
	};
});

vi.mock("@/lib/api/client", async importOriginal => {
	const actual = await importOriginal<typeof apiClientTypes>();
	return {
		...actual,
		registerUnauthorizedHandler: (handler: () => void) => mockRegister(handler)
	};
});

vi.mock("./router", async () => {
	const { createMemoryRouter } = await import("react-router");
	return {
		router: createMemoryRouter([{ path: "*", element: <div>router content</div> }])
	};
});

import { Providers } from "./providers";

describe("Providers", () => {
	beforeEach(() => {
		mockUseAuth0.mockReset();
		mockRegister.mockReset();
	});

	afterEach(() => {
		vi.restoreAllMocks();
	});

	it("should render the router and register the unauthorized handler", () => {
		// GIVEN
		const logout = vi.fn();
		mockUseAuth0.mockReturnValue({
			isAuthenticated: true,
			getAccessTokenSilently: vi.fn(),
			logout
		});
		const testSubject = Providers;
		const Subject = testSubject;

		// WHEN
		render(<Subject />);

		// THEN
		expect(screen.getByText("router content")).toBeInTheDocument();
		const handler = mockRegister.mock.calls[0]?.[0] as () => void;
		handler();
		expect(logout).toHaveBeenCalledWith({
			logoutParams: { returnTo: window.location.origin }
		});
	});

	it("should replace the unauthorized handler on unmount", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({
			isAuthenticated: false,
			getAccessTokenSilently: vi.fn(),
			logout: vi.fn()
		});
		const testSubject = Providers;
		const Subject = testSubject;
		const { unmount } = render(<Subject />);

		// WHEN
		unmount();

		// THEN
		expect(mockRegister).toHaveBeenCalledTimes(2);
		expect((mockRegister.mock.calls[1]?.[0] as () => void)()).toBeUndefined();
	});

	it("should redirect after login to the requested route or the dashboard", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({
			isAuthenticated: true,
			getAccessTokenSilently: vi.fn(),
			logout: vi.fn()
		});
		const replace = vi.fn();
		vi.stubGlobal("location", { ...window.location, replace, origin: "http://localhost" });
		const testSubject = Providers;
		const Subject = testSubject;
		render(<Subject />);

		// WHEN
		capturedRedirect?.({ returnTo: "/assets" });

		// THEN
		expect(replace).toHaveBeenCalledWith("/assets");

		// WHEN
		capturedRedirect?.(undefined);

		// THEN
		expect(replace).toHaveBeenCalledWith("/dashboard");
		vi.unstubAllGlobals();
	});
});
