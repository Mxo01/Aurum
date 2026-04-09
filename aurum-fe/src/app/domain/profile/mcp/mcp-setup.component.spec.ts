import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Directive, Input } from "@angular/core";
import { MockComponent, MockDirective, MockModule, MockProvider } from "ng-mocks";
import { FormsModule } from "@angular/forms";
import { Button } from "primeng/button";
import { Tooltip } from "primeng/tooltip";
import { SelectButton } from "primeng/selectbutton";
import { MessageService } from "primeng/api";
import { McpSetupComponent } from "./mcp-setup.component";

// eslint-disable-next-line @angular-eslint/directive-selector
@Directive({ selector: "[highlight]", standalone: true })
class HighlightStub {
	@Input() highlight!: string | null;
	@Input() language!: string;
}

const mockClipboard = { writeText: vi.fn() };

describe("McpSetupComponent", () => {
	let fixture: ComponentFixture<McpSetupComponent>;
	let testSubject: McpSetupComponent;
	let mockMessageService: MessageService;

	beforeEach(() => {
		mockClipboard.writeText.mockReset();
		mockClipboard.writeText.mockResolvedValue(undefined);
		Object.defineProperty(globalThis, "navigator", {
			writable: true,
			value: { clipboard: mockClipboard }
		});
		TestBed.configureTestingModule({
			imports: [
				McpSetupComponent,
				MockComponent(Button),
				MockDirective(Tooltip),
				HighlightStub,
				MockComponent(SelectButton),
				MockModule(FormsModule)
			],
			providers: [MockProvider(MessageService, { add: vi.fn() })]
		});
		fixture = TestBed.createComponent(McpSetupComponent);
		testSubject = fixture.componentInstance;
		mockMessageService = TestBed.inject(MessageService);
		fixture.detectChanges();
	});

	describe("mcpConfig", () => {
		it("should return a JSON string containing mcpServers for desktop mode", () => {
			// GIVEN
			testSubject.isMcpDesktop.set(true);

			// WHEN
			const expectedConfig = testSubject.mcpConfig;

			// THEN
			expect(expectedConfig).toContain("mcpServers");
		});

		it("should return the SSE URL directly for mobile/web mode", () => {
			// GIVEN
			testSubject.isMcpDesktop.set(false);

			// WHEN
			const expectedConfig = testSubject.mcpConfig;

			// THEN
			expect(expectedConfig).not.toContain("mcpServers");
			expect(expectedConfig).toContain("/sse");
		});
	});

	describe("copyConfig", () => {
		it("should call navigator.clipboard.writeText with the mcp config", async () => {
			// GIVEN
			testSubject.isMcpDesktop.set(false);

			// WHEN
			testSubject.copyConfig();
			await Promise.resolve();

			// THEN
			expect(mockClipboard.writeText).toHaveBeenCalledWith(expect.stringContaining("/sse"));
		});

		it("should add a success message after config copy", async () => {
			// WHEN
			testSubject.copyConfig();
			await Promise.resolve();

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "success" })
			);
		});

		it("should add an error message when config copy fails", async () => {
			// GIVEN
			mockClipboard.writeText.mockRejectedValue(new Error("denied"));

			// WHEN
			testSubject.copyConfig();
			await Promise.resolve();
			await Promise.resolve();

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "error" })
			);
		});
	});
});
