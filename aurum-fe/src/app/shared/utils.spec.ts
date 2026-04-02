import {
	createBarGradient,
	createGradientFill,
	escapeHtml,
	getChartTooltipStyle,
	getOrCreateTooltipEl,
	isTruthy,
	positionTooltipEl,
	tooltipContainer,
	tooltipRow
} from "./utils";

describe("isTruthy", () => {
	it.each([null, undefined, "", false])("should return false for %s", value => {
		// GIVEN / WHEN / THEN
		expect(isTruthy(value)).toBe(false);
	});

	it.each([true, "hello", 42, {}])("should return true for %s", value => {
		// GIVEN / WHEN / THEN
		expect(isTruthy(value)).toBe(true);
	});
});

describe("escapeHtml", () => {
	it("should escape HTML special characters", () => {
		// GIVEN
		const malicious = '<img src=x onerror=alert("XSS")>';

		// WHEN
		const result = escapeHtml(malicious);

		// THEN
		expect(result).toBe("&lt;img src=x onerror=alert(&quot;XSS&quot;)&gt;");
		expect(result).not.toContain("<");
		expect(result).not.toContain(">");
	});

	it("should escape ampersands and quotes", () => {
		// GIVEN / WHEN
		const result = escapeHtml('Tom & Jerry\'s "show"');

		// THEN
		expect(result).toBe("Tom &amp; Jerry&#39;s &quot;show&quot;");
	});

	it("should return the same string when no special characters", () => {
		// GIVEN / WHEN
		const result = escapeHtml("Hello World 123");

		// THEN
		expect(result).toBe("Hello World 123");
	});
});

describe("tooltipRow", () => {
	it("should escape the label to prevent XSS", () => {
		// GIVEN
		const style = getChartTooltipStyle(false);

		// WHEN
		const html = tooltipRow(style, "<span></span>", '<script>alert("xss")</script>', "$10,000");

		// THEN
		expect(html).not.toContain("<script>");
		expect(html).toContain("&lt;script&gt;");
	});
});

describe("tooltipContainer", () => {
	it("should escape the title to prevent XSS", () => {
		// GIVEN
		const style = getChartTooltipStyle(false);

		// WHEN
		const html = tooltipContainer(style, "<img src=x onerror=alert(1)>", "<div>body</div>");

		// THEN
		expect(html).not.toContain("<img src=x");
		expect(html).toContain("&lt;img");
	});
});

describe("getChartTooltipStyle", () => {
	it("should return a dark background in dark mode", () => {
		// GIVEN / WHEN
		const style = getChartTooltipStyle(true);

		// THEN
		expect(style.bg).toBe("#18181B");
	});

	it("should return a light background in light mode", () => {
		// GIVEN / WHEN
		const style = getChartTooltipStyle(false);

		// THEN
		expect(style.bg).toBe("#f4f4f5");
	});

	it("should return white title color in dark mode", () => {
		// GIVEN / WHEN
		const style = getChartTooltipStyle(true);

		// THEN
		expect(style.title).toBe("#ffffff");
	});
});

describe("tooltipRow", () => {
	it("should include the label and value in the returned HTML", () => {
		// GIVEN
		const style = getChartTooltipStyle(false);

		// WHEN
		const html = tooltipRow(style, "<span></span>", "Net Worth", "$10,000");

		// THEN
		expect(html).toContain("Net Worth");
		expect(html).toContain("$10,000");
	});
});

describe("tooltipContainer", () => {
	it("should include the title and body in the returned HTML", () => {
		// GIVEN
		const style = getChartTooltipStyle(false);

		// WHEN
		const html = tooltipContainer(style, "January 2024", "<div>row</div>");

		// THEN
		expect(html).toContain("January 2024");
		expect(html).toContain("<div>row</div>");
	});
});

describe("getOrCreateTooltipEl", () => {
	afterEach(() => {
		document.getElementById("chartjs-tooltip-test-1")?.remove();
	});

	it("should create a new div and append it to the body when it does not exist", () => {
		// GIVEN / WHEN
		const el = getOrCreateTooltipEl("test-1");

		// THEN
		expect(el).toBeInstanceOf(HTMLDivElement);
		expect(document.body.contains(el)).toBe(true);
	});

	it("should return the existing element when called a second time with the same id", () => {
		// GIVEN
		const first = getOrCreateTooltipEl("test-1");

		// WHEN
		const second = getOrCreateTooltipEl("test-1");

		// THEN
		expect(second).toBe(first);
	});
});

describe("createBarGradient", () => {
	it("should return topColor when chart has no chartArea", () => {
		// GIVEN
		const fn = createBarGradient("red", "blue");

		// WHEN
		const result = fn({ chart: { ctx: {}, chartArea: null } });

		// THEN
		expect(result).toBe("red");
	});

	it("should return a gradient when chart area is available", () => {
		// GIVEN
		const mockGradient = { addColorStop: vi.fn() };
		const mockCtx = { createLinearGradient: vi.fn().mockReturnValue(mockGradient) };
		const fn = createBarGradient("red", "blue");

		// WHEN
		const result = fn({ chart: { ctx: mockCtx, chartArea: { top: 0, bottom: 100 } } });

		// THEN
		expect(result).toBe(mockGradient);
		expect(mockCtx.createLinearGradient).toHaveBeenCalled();
	});
});

describe("createGradientFill", () => {
	it("should return 'transparent' when chart has no chartArea", () => {
		// GIVEN
		const fn = createGradientFill("#abc123");

		// WHEN
		const result = fn({ chart: { ctx: {}, chartArea: null } });

		// THEN
		expect(result).toBe("transparent");
	});

	it("should return a gradient when chart area is available", () => {
		// GIVEN
		const mockGradient = { addColorStop: vi.fn() };
		const mockCtx = { createLinearGradient: vi.fn().mockReturnValue(mockGradient) };
		const fn = createGradientFill("#abc123");

		// WHEN
		const result = fn({ chart: { ctx: mockCtx, chartArea: { top: 0, bottom: 100 } } });

		// THEN
		expect(result).toBe(mockGradient);
		expect(mockGradient.addColorStop).toHaveBeenCalledTimes(2);
	});
});

describe("positionTooltipEl", () => {
	it("should set opacity to 1 and apply positioning", () => {
		// GIVEN
		const el = document.createElement("div");
		document.body.appendChild(el);
		const rect = { left: 100, top: 200 } as DOMRect;

		// WHEN
		positionTooltipEl(el, rect, 50, 80);

		// THEN
		expect(el.style.opacity).toBe("1");
		expect(el.style.left).toBeTruthy();
		expect(el.style.top).toBeTruthy();

		el.remove();
	});

	it("should pin to the left edge when the tooltip would overflow the left side", () => {
		// GIVEN
		Object.defineProperty(window, "innerWidth", { value: 1200, writable: true });
		const el = document.createElement("div");
		document.body.appendChild(el);
		const rect = { left: 0, top: 100 } as DOMRect;

		// WHEN
		positionTooltipEl(el, rect, 0, 50);

		// THEN
		expect(el.style.left).toBe("8px");

		el.remove();
	});

	it("should position below the caret when the tooltip would overflow the top", () => {
		// GIVEN
		const el = document.createElement("div");
		document.body.appendChild(el);
		const rect = { left: 100, top: 0 } as DOMRect;

		// WHEN
		positionTooltipEl(el, rect, 50, 0);

		// THEN
		expect(el.style.top).toBe("12px");

		el.remove();
	});

	it("should pin to the right edge when the tooltip would overflow the right side", () => {
		// GIVEN
		Object.defineProperty(window, "innerWidth", { value: 300, writable: true });
		const el = document.createElement("div");
		Object.defineProperty(el, "offsetWidth", { get: () => 400 });
		document.body.appendChild(el);
		const rect = { left: 200, top: 100 } as DOMRect;

		// WHEN
		positionTooltipEl(el, rect, 50, 50);

		// THEN
		expect(el.style.left).toBe(`${300 - 400 - 8}px`);

		el.remove();
	});
});
