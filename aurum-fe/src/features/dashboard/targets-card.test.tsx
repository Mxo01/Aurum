import { act, fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { env } from "@/lib/env";
import { toDateOnly } from "@/lib/format/date";
import { buildTarget } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { TargetsCard } from "./targets-card";

const mockTargets = [
	buildTarget({
		id: "t1",
		name: "Emergency fund",
		targetAmount: 50000,
		currentAmount: 20000,
		completionPercentage: 40,
		status: "IN_PROGRESS",
		type: "MANUAL"
	}),
	buildTarget({
		id: "t2",
		name: "New car",
		targetAmount: 30000,
		currentAmount: 30000,
		completionPercentage: 100,
		status: "COMPLETED"
	}),
	buildTarget({
		id: "t3",
		name: "Sabbatical",
		targetAmount: 10000,
		currentAmount: 1000,
		completionPercentage: 10,
		status: "NOT_REACHED"
	})
];

function renderCard(props: Partial<Parameters<typeof TargetsCard>[0]> = {}, route = "/dashboard") {
	return renderWithProviders(
		createElement(TargetsCard, {
			targets: mockTargets,
			isLoading: false,
			currency: "EUR",
			...props
		}),
		{ route }
	);
}

function targetsUrl() {
	return `${env.apiUrl}/targets`;
}

let resizeCallback: ResizeObserverCallback | undefined;
let disconnectSpy = vi.fn();

function stubResizeObserver() {
	disconnectSpy = vi.fn();
	class MockResizeObserver {
		constructor(callback: ResizeObserverCallback) {
			resizeCallback = callback;
		}
		observe() {}
		unobserve() {}
		disconnect() {
			disconnectSpy();
		}
	}
	vi.stubGlobal("ResizeObserver", MockResizeObserver);
}

function setListHeight(scrollHeight: number, clientHeight: number) {
	Object.defineProperty(HTMLElement.prototype, "scrollHeight", {
		configurable: true,
		get: () => scrollHeight
	});
	Object.defineProperty(HTMLElement.prototype, "clientHeight", {
		configurable: true,
		get: () => clientHeight
	});
}

describe("TargetsCard", () => {
	beforeEach(() => {
		window.localStorage.clear();
		resizeCallback = undefined;
		setListHeight(0, 0);
	});

	afterEach(() => {
		vi.restoreAllMocks();
		vi.unstubAllGlobals();
	});

	it("should show the loading skeletons", () => {
		// GIVEN
		const testSubject = TargetsCard;

		// WHEN
		const { container } = renderCard({ targets: undefined, isLoading: true });

		// THEN
		expect(container.querySelectorAll(".animate-pulse").length).toBeGreaterThan(0);
		expect(testSubject).toBeDefined();
	});

	it("should show the empty state and open the form from it", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({ targets: [] });

		// WHEN
		await user.click(screen.getByRole("button", { name: /Set a target/ }));

		// THEN
		expect(await screen.findByRole("heading", { name: "New target" })).toBeInTheDocument();
	});

	it("should list the targets with their status badges", () => {
		// GIVEN
		const testSubject = TargetsCard;

		// WHEN
		renderCard();

		// THEN
		expect(screen.getByText("Emergency fund")).toBeInTheDocument();
		expect(screen.getByText("In progress")).toBeInTheDocument();
		expect(screen.getByText("Completed")).toBeInTheDocument();
		expect(screen.getByText("Not reached")).toBeInTheDocument();
		expect(screen.getByText("40%")).toBeInTheDocument();
		expect(screen.getByText("€30,000 to go")).toBeInTheDocument();
		expect(testSubject).toBeDefined();
	});

	it("should fall back to in progress without a status", () => {
		// GIVEN
		const noStatus = buildTarget({ id: "t4", name: "No status", status: undefined });

		// WHEN
		renderCard({ targets: [noStatus] });

		// THEN
		expect(screen.getByText("In progress")).toBeInTheDocument();
	});

	it("should clamp the completion between zero and one hundred", () => {
		// GIVEN
		const over = buildTarget({
			id: "t5",
			name: "Over",
			completionPercentage: 150,
			currentAmount: 60000,
			targetAmount: 50000
		});
		const under = buildTarget({
			id: "t6",
			name: "Under",
			completionPercentage: -20,
			currentAmount: 0,
			targetAmount: 50000
		});

		// WHEN
		renderCard({ targets: [over, under] });

		// THEN
		expect(screen.getByText("100%")).toBeInTheDocument();
		expect(screen.getByText("0%")).toBeInTheDocument();
		expect(screen.getByText("€0 to go")).toBeInTheDocument();
	});

	it("should create a manual target", async () => {
		// GIVEN
		let postedBody: unknown;
		server.use(
			http.post(targetsUrl(), async ({ request }) => {
				postedBody = await request.json();
				return HttpResponse.json(buildTarget({ name: "New car" }));
			})
		);
		const user = userEvent.setup();
		renderCard({ targets: [] });
		await user.click(screen.getByRole("button", { name: /Set a target/ }));
		await screen.findByRole("heading", { name: "New target" });

		// WHEN
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "New car" } });
		fireEvent.change(screen.getByLabelText("Target amount"), { target: { value: "30000" } });
		fireEvent.change(screen.getByLabelText("Current saved amount"), {
			target: { value: "5000" }
		});
		const submit = screen.getByRole("button", { name: "Create target" });
		await waitFor(() => expect(submit).toBeEnabled());
		fireEvent.submit(submit.closest("form") as HTMLFormElement);

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).toMatchObject({
			name: "New car",
			type: "MANUAL",
			targetAmount: 30000,
			currentAmount: 5000
		});
	});

	it("should keep the submit disabled until the form is valid", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({ targets: [] });
		await user.click(screen.getByRole("button", { name: /Set a target/ }));
		const submit = screen.getByRole("button", { name: "Create target" });

		// THEN
		expect(submit).toBeDisabled();

		// WHEN
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "New car" } });
		fireEvent.change(screen.getByLabelText("Target amount"), { target: { value: "30000" } });

		// THEN
		await waitFor(() => expect(submit).toBeEnabled());
	});

	it("should send no current amount for a net worth target", async () => {
		// GIVEN
		let postedBody: unknown;
		server.use(
			http.post(targetsUrl(), async ({ request }) => {
				postedBody = await request.json();
				return HttpResponse.json(buildTarget({ type: "NET_WORTH" }));
			})
		);
		const user = userEvent.setup();
		renderCard({ targets: [] });
		await user.click(screen.getByRole("button", { name: /Set a target/ }));
		await screen.findByRole("heading", { name: "New target" });

		// WHEN
		await user.click(screen.getByLabelText("Type"));
		await user.click(await screen.findByRole("option", { name: "Net worth" }));
		expect(screen.queryByLabelText("Current saved amount")).not.toBeInTheDocument();
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Net worth" } });
		fireEvent.change(screen.getByLabelText("Target amount"), { target: { value: "1000000" } });
		const submit = screen.getByRole("button", { name: "Create target" });
		await waitFor(() => expect(submit).toBeEnabled());
		fireEvent.submit(submit.closest("form") as HTMLFormElement);

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).toMatchObject({ type: "NET_WORTH" });
		expect(postedBody).not.toHaveProperty("currentAmount");
	});

	it("should show the validation errors", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({ targets: [] });
		await user.click(screen.getByRole("button", { name: /Set a target/ }));
		const form = screen.getByRole("button", { name: "Create target" }).closest("form");

		// WHEN
		fireEvent.submit(form as HTMLFormElement);

		// THEN
		expect(await screen.findByText("Name is required")).toBeInTheDocument();
		expect(screen.getByText("Enter an amount greater than zero")).toBeInTheDocument();
	});

	it("should edit the active target from the menu", async () => {
		// GIVEN
		let updatedBody: unknown;
		server.use(
			http.put(`${targetsUrl()}/:id`, async ({ request }) => {
				updatedBody = await request.json();
				return HttpResponse.json(mockTargets[0]);
			})
		);
		const user = userEvent.setup();
		renderCard();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Target actions" }));
		await user.click(await screen.findByRole("menuitem", { name: /Edit target/ }));
		const name = screen.getByLabelText("Name");
		expect(name).toHaveValue("Emergency fund");
		fireEvent.change(name, { target: { value: "Rainy day" } });
		const submit = screen.getByRole("button", { name: "Save changes" });
		await waitFor(() => expect(submit).toBeEnabled());
		fireEvent.submit(submit.closest("form") as HTMLFormElement);

		// THEN
		await waitFor(() => expect(updatedBody).toBeDefined());
		expect(updatedBody).toMatchObject({ name: "Rainy day", currentAmount: 20000 });
	});

	it("should add a target from the menu", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Target actions" }));
		await user.click(await screen.findByRole("menuitem", { name: /Add target/ }));

		// THEN
		expect(await screen.findByRole("heading", { name: "New target" })).toBeInTheDocument();
	});

	it("should reset the form when it is closed", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({ targets: [] });
		await user.click(screen.getByRole("button", { name: /Set a target/ }));
		fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Draft" } });

		// WHEN
		await user.click(screen.getByRole("button", { name: "Close" }));
		await waitFor(() =>
			expect(screen.queryByRole("heading", { name: "New target" })).not.toBeInTheDocument()
		);
		await user.click(screen.getByRole("button", { name: /Set a target/ }));

		// THEN
		expect(screen.getByLabelText("Name")).toHaveValue("");
	});

	it("should open the form from the URL and clear the parameter on close", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({ targets: [] }, "/dashboard?target=new");

		// THEN
		expect(await screen.findByRole("heading", { name: "New target" })).toBeInTheDocument();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Close" }));

		// THEN
		await waitFor(() =>
			expect(screen.queryByRole("heading", { name: "New target" })).not.toBeInTheDocument()
		);
	});

	it("should list every target in the sheet and edit one", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Target actions" }));
		await user.click(await screen.findByRole("menuitem", { name: /View all targets/ }));
		const sheet = await screen.findByRole("heading", { name: "All targets" });

		// THEN
		expect(sheet).toBeInTheDocument();
		expect(screen.getAllByText("Sabbatical").length).toBeGreaterThan(0);

		// WHEN
		await user.click(screen.getByRole("button", { name: "Edit New car" }));

		// THEN
		expect(await screen.findByRole("heading", { name: "Edit target" })).toBeInTheDocument();
		expect(screen.getByLabelText("Name")).toHaveValue("New car");
	});

	it("should delete a target from the sheet", async () => {
		// GIVEN
		let deletedId: string | undefined;
		server.use(
			http.delete(`${targetsUrl()}/:id`, ({ params }) => {
				deletedId = String(params.id);
				return new HttpResponse(null, { status: 204 });
			})
		);
		const user = userEvent.setup();
		renderCard();
		await user.click(screen.getByRole("button", { name: "Target actions" }));
		await user.click(await screen.findByRole("menuitem", { name: /View all targets/ }));
		await screen.findByRole("heading", { name: "All targets" });

		// WHEN
		await user.click(screen.getByRole("button", { name: "Delete New car" }));
		const dialog = await screen.findByRole("alertdialog");
		fireEvent.click(within(dialog).getByRole("button", { name: "Delete" }));

		// THEN
		await waitFor(() => expect(deletedId).toBe("t2"));
	});

	it("should show the empty fallback in the list sheet", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({ targets: [] });

		// WHEN
		await user.click(screen.getByRole("button", { name: "Target actions" }));
		await user.click(await screen.findByRole("menuitem", { name: /View all targets/ }));

		// THEN
		expect(await screen.findByText("No targets yet.")).toBeInTheDocument();
	});

	it("should render a target without id, current amount or completion", () => {
		// GIVEN
		const bare = buildTarget({
			id: null,
			name: "Bare target",
			currentAmount: null,
			completionPercentage: null,
			status: null
		});

		// WHEN
		renderCard({ targets: [bare] });

		// THEN
		expect(screen.getByText("Bare target")).toBeInTheDocument();
		expect(screen.getByText("€0 / €50,000")).toBeInTheDocument();
		expect(screen.getByText("0%")).toBeInTheDocument();
		expect(screen.getByText("In progress")).toBeInTheDocument();
	});

	it("should show the negative current amount error", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({ targets: [] });
		await user.click(screen.getByRole("button", { name: /Set a target/ }));

		// WHEN
		fireEvent.change(screen.getByLabelText("Current saved amount"), {
			target: { value: "-5" }
		});
		fireEvent.submit(
			screen.getByRole("button", { name: "Create target" }).closest("form") as HTMLFormElement
		);

		// THEN
		expect(await screen.findByText("Cannot be negative")).toBeInTheDocument();
	});

	it("should edit a net worth target without a current amount", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({
			targets: [
				buildTarget({
					id: "t7",
					name: "Net worth",
					type: "NET_WORTH",
					currentAmount: null
				})
			]
		});

		// WHEN
		await user.click(screen.getByRole("button", { name: "Target actions" }));
		await user.click(await screen.findByRole("menuitem", { name: /Edit target/ }));

		// THEN
		expect(await screen.findByRole("heading", { name: "Edit target" })).toBeInTheDocument();
		expect(screen.queryByLabelText("Current saved amount")).not.toBeInTheDocument();
	});

	it("should open the list from the view all overlay and add a target", async () => {
		// GIVEN
		stubResizeObserver();
		setListHeight(500, 100);
		const bare = buildTarget({
			id: null,
			name: "Bare target",
			currentAmount: null,
			completionPercentage: null
		});
		const user = userEvent.setup();
		renderCard({ targets: [bare] });

		// WHEN
		await user.click(screen.getByRole("button", { name: "View all" }));

		// THEN
		expect(await screen.findByRole("heading", { name: "All targets" })).toBeInTheDocument();
		const dialog = screen.getByRole("dialog");
		expect(within(dialog).getByText("Bare target")).toBeInTheDocument();

		// WHEN
		await user.click(screen.getByRole("button", { name: /Add target/ }));

		// THEN
		expect(await screen.findByRole("heading", { name: "New target" })).toBeInTheDocument();
	});

	it("should show the view all overlay when the list is clipped", () => {
		// GIVEN
		stubResizeObserver();
		setListHeight(500, 100);

		// WHEN
		renderCard();

		// THEN
		expect(screen.getByRole("button", { name: "View all" })).toBeInTheDocument();

		// WHEN
		act(() => {
			resizeCallback?.([], {} as ResizeObserver);
		});

		// THEN
		expect(screen.getByRole("button", { name: "View all" })).toBeInTheDocument();
	});

	it("should hide the view all overlay when the list fits", () => {
		// GIVEN
		stubResizeObserver();
		setListHeight(100, 500);

		// WHEN
		renderCard();

		// THEN
		expect(screen.queryByRole("button", { name: "View all" })).not.toBeInTheDocument();
	});

	it("should disconnect the observer on unmount", () => {
		// GIVEN
		stubResizeObserver();
		setListHeight(500, 100);
		const { unmount } = renderCard();

		// WHEN
		unmount();

		// THEN
		expect(disconnectSpy).toHaveBeenCalled();
	});

	it("should render without a ResizeObserver", () => {
		// GIVEN
		vi.stubGlobal("ResizeObserver", undefined);
		setListHeight(500, 100);

		// WHEN
		renderCard();

		// THEN
		expect(screen.getByRole("button", { name: "View all" })).toBeInTheDocument();
	});

	it("should constrain the deadline to tomorrow", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderCard({ targets: [] });
		await user.click(screen.getByRole("button", { name: /Set a target/ }));

		// WHEN
		await user.click(screen.getByRole("button", { name: "Deadline" }));

		// THEN
		const today = new Date();
		const yesterday = new Date(today);
		yesterday.setDate(yesterday.getDate() - 1);
		const tomorrow = new Date(today);
		tomorrow.setDate(tomorrow.getDate() + 1);
		const disabledCell = document.querySelector(`[data-day="${toDateOnly(yesterday)}"]`);
		const enabledCell = document.querySelector(`[data-day="${toDateOnly(tomorrow)}"]`);
		expect(disabledCell).toHaveAttribute("data-disabled", "true");
		expect(enabledCell).not.toHaveAttribute("data-disabled", "true");
	});
});
