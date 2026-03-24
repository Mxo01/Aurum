import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MockProvider } from "ng-mocks";
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

	beforeEach(() => {
		mockClipboard.writeText.mockReset();
		mockClipboard.writeText.mockResolvedValue(undefined);
		Object.defineProperty(globalThis, "navigator", {
			writable: true,
			value: { clipboard: mockClipboard }
		});
		TestBed.overrideComponent(McpSetupComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({
			imports: [McpSetupComponent],
			providers: [
				MockProvider(McpService, {
					getKeyMeta: vi.fn().mockReturnValue(of(null)),
					generateKey: vi.fn().mockReturnValue(of({ key: "test-key" } as GeneratedKey)),
					revokeKey: vi.fn().mockReturnValue(of({})),
					mcpSseUrl: "http://test-api/sse"
				}),
				MockProvider(MessageService, { add: vi.fn() })
			]
		});
		fixture = TestBed.createComponent(McpSetupComponent);
		testSubject = fixture.componentInstance;
		mockMcpService = TestBed.inject(McpService);
		fixture.detectChanges();
	});

	describe("ngOnInit", () => {
		it("should load key metadata on init", () => {
			// GIVEN
			const mockMeta = buildMockKeyMeta();
			vi.spyOn(mockMcpService, "getKeyMeta").mockReturnValue(of(mockMeta));
			fixture = TestBed.createComponent(McpSetupComponent);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(fixture.componentInstance.keyMeta()?.id).toBe(mockMeta.id);
		});

		it("should set keyMeta to null when getKeyMeta returns an error", () => {
			// GIVEN
			vi.spyOn(mockMcpService, "getKeyMeta").mockReturnValue(
				throwError(() => new Error("not found"))
			);
			fixture = TestBed.createComponent(McpSetupComponent);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(fixture.componentInstance.keyMeta()).toBeNull();
		});
	});

	describe("generateKey", () => {
		it("should set generatedKey and open instructions dialog on success", () => {
			// GIVEN
			vi.spyOn(mockMcpService, "generateKey").mockReturnValue(
				of({ key: "new-key" } as GeneratedKey)
			);
			vi.spyOn(mockMcpService, "getKeyMeta").mockReturnValue(of(buildMockKeyMeta()));

			// WHEN
			testSubject.generateKey();
			fixture.detectChanges();

			// THEN
			expect(testSubject.generatedKey()).toBe("new-key");
			expect(testSubject.isApiKeyInstructionsDialogVisible()).toBe(true);
		});

		it("should add an error message when key generation fails", () => {
			// GIVEN
			const mockMessageService = TestBed.inject(MessageService);
			vi.spyOn(mockMcpService, "generateKey").mockReturnValue(throwError(() => new Error("fail")));

			// WHEN
			testSubject.generateKey();
			fixture.detectChanges();

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "error" })
			);
		});
	});

	describe("revokeKey", () => {
		it("should clear keyMeta on successful revocation", () => {
			// GIVEN
			vi.spyOn(mockMcpService, "revokeKey").mockReturnValue(of({}));
			testSubject.keyMeta.set(buildMockKeyMeta());

			// WHEN
			testSubject.revokeKey();
			fixture.detectChanges();

			// THEN
			expect(testSubject.keyMeta()).toBeNull();
		});

		it("should add an error message when revocation fails", () => {
			// GIVEN
			const mockMessageService = TestBed.inject(MessageService);
			vi.spyOn(mockMcpService, "revokeKey").mockReturnValue(throwError(() => new Error("fail")));

			// WHEN
			testSubject.revokeKey();
			fixture.detectChanges();

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "error" })
			);
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
			const mockMessageService = TestBed.inject(MessageService);
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
			const mockMessageService = TestBed.inject(MessageService);
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
			// GIVEN
			const mockMessageService = TestBed.inject(MessageService);

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
			const mockMessageService = TestBed.inject(MessageService);
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
	});
});
