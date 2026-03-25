import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Directive, Input } from "@angular/core";
import { MockComponent, MockDirective, MockModule, MockPipe, MockProvider } from "ng-mocks";
import { DatePipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Button } from "primeng/button";
import { Dialog } from "primeng/dialog";
import { Tooltip } from "primeng/tooltip";
import { Message } from "primeng/message";
import { SelectButton } from "primeng/selectbutton";
import { provideHighlightOptions } from "ngx-highlightjs";

// eslint-disable-next-line @angular-eslint/directive-selector
@Directive({ selector: "[highlight]", standalone: true })
class HighlightStub {
	@Input() highlight!: string | null;
	@Input() language!: string;
}
import { faker } from "@faker-js/faker";
import { of, throwError } from "rxjs";
import { McpSetupComponent } from "./mcp-setup.component";
import { McpService } from "./mcp.service";
import { MessageService } from "primeng/api";
import { ApiKeyMeta, GeneratedKey } from "./model/mcp.model";

const mockClipboard = { writeText: vi.fn() };

const buildMockKeyMeta = (overrides: Partial<ApiKeyMeta> = {}): ApiKeyMeta => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	createdAt: faker.date.past().toISOString(),
	lastUsedAt: null,
	...overrides
});

describe("McpSetupComponent", () => {
	let fixture: ComponentFixture<McpSetupComponent>;
	let testSubject: McpSetupComponent;
	let mockMcpService: McpService;
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
				MockComponent(Dialog),
				MockPipe(DatePipe),
				MockDirective(Tooltip),
				MockComponent(Message),
				HighlightStub,
				MockComponent(SelectButton),
				MockModule(FormsModule)
			],
			providers: [
				MockProvider(McpService, {
					getKeyMeta: vi.fn().mockReturnValue(of(null)),
					generateKey: vi.fn().mockReturnValue(of({ key: "test-key" } as GeneratedKey)),
					revokeKey: vi.fn().mockReturnValue(of({})),
					mcpSseUrl: "http://test-api/sse"
				}),
				MockProvider(MessageService, { add: vi.fn() }),
				provideHighlightOptions({ fullLibraryLoader: () => Promise.resolve({}) })
			]
		});
		fixture = TestBed.createComponent(McpSetupComponent);
		testSubject = fixture.componentInstance;
		mockMcpService = TestBed.inject(McpService);
		mockMessageService = TestBed.inject(MessageService);
		fixture.detectChanges();
	});

	describe("ngOnInit", () => {
		it("should load key metadata on init", () => {
			// GIVEN
			const mockMeta = buildMockKeyMeta();
			vi.spyOn(mockMcpService, "getKeyMeta").mockReturnValue(of(mockMeta));

			// WHEN
			testSubject.ngOnInit();

			// THEN
			expect(testSubject.keyMeta()).toEqual(mockMeta);
		});

		it("should set keyMeta to null when getKeyMeta returns an error", () => {
			// GIVEN
			vi.spyOn(mockMcpService, "getKeyMeta").mockReturnValue(
				throwError(() => new Error("not found"))
			);

			// WHEN
			testSubject.ngOnInit();

			// THEN
			expect(testSubject.keyMeta()).toBeNull();
		});
	});

	describe("generateKey", () => {
		it("should set generatedKey, open instructions dialog, and update keyMeta on success", () => {
			// GIVEN
			const mockMeta = buildMockKeyMeta();
			vi.spyOn(mockMcpService, "generateKey").mockReturnValue(
				of({ key: "new-key" } as GeneratedKey)
			);
			vi.spyOn(mockMcpService, "getKeyMeta").mockReturnValue(of(mockMeta));

			// WHEN
			testSubject.generateKey();
			fixture.detectChanges();

			// THEN
			expect(testSubject.generatedKey()).toBe("new-key");
			expect(testSubject.isApiKeyInstructionsDialogVisible()).toBe(true);
			expect(testSubject.keyMeta()).toEqual(mockMeta);
			expect(testSubject.isGenerating()).toBe(false);
		});

		it("should add an error message and reset isGenerating when key generation fails", () => {
			// GIVEN
			vi.spyOn(mockMcpService, "generateKey").mockReturnValue(throwError(() => new Error("fail")));

			// WHEN
			testSubject.generateKey();
			fixture.detectChanges();

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "error" })
			);
			expect(testSubject.isGenerating()).toBe(false);
		});
	});

	describe("revokeKey", () => {
		it("should clear keyMeta, add a success message, and reset isRevoking on successful revocation", () => {
			// GIVEN
			vi.spyOn(mockMcpService, "revokeKey").mockReturnValue(of({}));
			testSubject.keyMeta.set(buildMockKeyMeta());

			// WHEN
			testSubject.revokeKey();
			fixture.detectChanges();

			// THEN
			expect(testSubject.keyMeta()).toBeNull();
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "success" })
			);
			expect(testSubject.isRevoking()).toBe(false);
		});

		it("should add an error message and reset isRevoking when revocation fails", () => {
			// GIVEN
			vi.spyOn(mockMcpService, "revokeKey").mockReturnValue(throwError(() => new Error("fail")));

			// WHEN
			testSubject.revokeKey();
			fixture.detectChanges();

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "error" })
			);
			expect(testSubject.isRevoking()).toBe(false);
		});
	});

	describe("copyKey", () => {
		it("should call navigator.clipboard.writeText with the generated key", async () => {
			// GIVEN
			testSubject.generatedKey.set("my-api-key");

			// WHEN
			testSubject.copyKey();
			await Promise.resolve();

			// THEN
			expect(mockClipboard.writeText).toHaveBeenCalledWith("my-api-key");
		});

		it("should not call clipboard when there is no generated key", () => {
			// GIVEN
			testSubject.generatedKey.set(null);

			// WHEN
			testSubject.copyKey();

			// THEN
			expect(mockClipboard.writeText).not.toHaveBeenCalled();
		});

		it("should add a success message after copy", async () => {
			// GIVEN
			testSubject.generatedKey.set("my-api-key");

			// WHEN
			testSubject.copyKey();
			await Promise.resolve();

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "success" })
			);
		});

		it("should add an error message when copy fails", async () => {
			// GIVEN
			mockClipboard.writeText.mockRejectedValue(new Error("denied"));
			testSubject.generatedKey.set("my-api-key");

			// WHEN
			testSubject.copyKey();
			await Promise.resolve();
			await Promise.resolve();

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "error" })
			);
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

	describe("confirmSaved", () => {
		it("should close the instructions dialog and clear the generated key", () => {
			// GIVEN
			testSubject.isApiKeyInstructionsDialogVisible.set(true);
			testSubject.generatedKey.set("some-key");

			// WHEN
			testSubject.confirmSaved();

			// THEN
			expect(testSubject.isApiKeyInstructionsDialogVisible()).toBe(false);
			expect(testSubject.generatedKey()).toBeNull();
		});
	});

	describe("mcpConfig", () => {
		it("should return a JSON string for desktop mode", () => {
			// GIVEN
			testSubject.isMcpDesktop.set(true);

			// WHEN
			const expectedConfig = testSubject.mcpConfig();

			// THEN
			expect(expectedConfig).toContain("mcpServers");
		});

		it("should return the SSE URL directly for mobile/web mode", () => {
			// GIVEN
			testSubject.isMcpDesktop.set(false);

			// WHEN
			const expectedConfig = testSubject.mcpConfig();

			// THEN
			expect(expectedConfig).not.toContain("mcpServers");
			expect(expectedConfig).toContain("/sse");
		});

		it("should embed the generated key in the config URL when available", () => {
			// GIVEN
			testSubject.generatedKey.set("actual-key");
			testSubject.isMcpDesktop.set(false);

			// WHEN
			const expectedConfig = testSubject.mcpConfig();

			// THEN
			expect(expectedConfig).toContain("actual-key");
		});

		it("should use the placeholder key in the config URL when no generated key is set", () => {
			// GIVEN
			testSubject.generatedKey.set(null);
			testSubject.isMcpDesktop.set(false);

			// WHEN
			const expectedConfig = testSubject.mcpConfig();

			// THEN
			expect(expectedConfig).toContain("<your-api-key>");
		});
	});
});
