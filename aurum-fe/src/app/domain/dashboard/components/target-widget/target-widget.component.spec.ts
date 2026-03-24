import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { MockProvider } from "ng-mocks";
import { faker } from "@faker-js/faker";
import { of } from "rxjs";
import { TargetWidgetComponent } from "./target-widget.component";
import { TargetService } from "../../../target/target.service";
import { ThemeService } from "../../../../shared/services/theme/theme.service";
import { Target, TargetType } from "../../../target/model/target.model";
import { Currency } from "../../../profile/model/currency.model";

const buildMockTarget = (overrides: Partial<Target> = {}): Target => ({
	id: faker.string.uuid(),
	name: faker.lorem.words(2),
	targetAmount: faker.number.float({ min: 1000, max: 100000 }),
	currentAmount: faker.number.float({ min: 0, max: 999 }),
	deadline: faker.date.future().toISOString().split("T")[0],
	type: TargetType.MANUAL,
	...overrides
});

describe("TargetWidgetComponent", () => {
	let fixture: ComponentFixture<TargetWidgetComponent>;
	let testSubject: TargetWidgetComponent;
	let mockTargetService: TargetService;

	beforeEach(() => {
		TestBed.overrideComponent(TargetWidgetComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({
			imports: [TargetWidgetComponent],
			providers: [
				MockProvider(TargetService, {
					saveTarget: vi.fn().mockReturnValue(of([])),
					deleteTarget: vi.fn().mockReturnValue(of([]))
				}),
				MockProvider(ThemeService, { isDarkMode: signal(false) })
			]
		});
		fixture = TestBed.createComponent(TargetWidgetComponent);
		testSubject = fixture.componentInstance;
		mockTargetService = TestBed.inject(TargetService);
		fixture.componentRef.setInput("targets", []);
		fixture.componentRef.setInput("currency", Currency.EUR);
		fixture.detectChanges();
	});

	describe("activeTarget", () => {
		it("should return the first target when targets are provided", () => {
			// GIVEN
			const mockTargets = [buildMockTarget(), buildMockTarget()];
			fixture.componentRef.setInput("targets", mockTargets);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.activeTarget()?.id).toBe(mockTargets[0].id);
		});

		it("should return null when there are no targets", () => {
			// GIVEN
			fixture.componentRef.setInput("targets", []);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.activeTarget()).toBeNull();
		});
	});

	describe("onAddTarget", () => {
		it("should clear selectedTarget and open the form", () => {
			// WHEN
			testSubject.onAddTarget();

			// THEN
			expect(testSubject.selectedTarget()).toBeNull();
			expect(testSubject.isFormVisible()).toBe(true);
		});
	});

	describe("onEditTarget", () => {
		it("should set selectedTarget and open the form", () => {
			// GIVEN
			const mockTarget = buildMockTarget();

			// WHEN
			testSubject.onEditTarget(mockTarget);

			// THEN
			expect(testSubject.selectedTarget()?.id).toBe(mockTarget.id);
			expect(testSubject.isFormVisible()).toBe(true);
		});
	});

	describe("onSaveTarget", () => {
		it("should call targetService.saveTarget and update targets list on success", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			const stubbedTargets = [mockTarget];
			vi.spyOn(mockTargetService, "saveTarget").mockReturnValue(of(stubbedTargets));

			// WHEN
			testSubject.onSaveTarget(mockTarget);
			fixture.detectChanges();

			// THEN
			expect(testSubject.targets()).toHaveLength(1);
			expect(testSubject.isFormVisible()).toBe(false);
		});
	});

	describe("menuItems", () => {
		it("should call onAddTarget when first menu item command is invoked and there is no active target", () => {
			// GIVEN
			fixture.componentRef.setInput("targets", []);
			fixture.detectChanges();

			// WHEN
			testSubject.menuItems()[0].command!({} as never);

			// THEN
			expect(testSubject.isFormVisible()).toBe(true);
			expect(testSubject.selectedTarget()).toBeNull();
		});

		it("should call onEditTarget when first menu item command is invoked and there is an active target", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			fixture.componentRef.setInput("targets", [mockTarget]);
			fixture.detectChanges();

			// WHEN
			testSubject.menuItems()[0].command!({} as never);

			// THEN
			expect(testSubject.isFormVisible()).toBe(true);
			expect(testSubject.selectedTarget()?.id).toBe(mockTarget.id);
		});

		it("should call onAddTarget when the second menu item command is invoked", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			fixture.componentRef.setInput("targets", [mockTarget]);
			fixture.detectChanges();

			// WHEN
			testSubject.menuItems()[1].command!({} as never);

			// THEN
			expect(testSubject.isFormVisible()).toBe(true);
			expect(testSubject.selectedTarget()).toBeNull();
		});

		it("should open the list when the View All Targets menu item is invoked", () => {
			// WHEN
			testSubject.menuItems()[2].command!({} as never);

			// THEN
			expect(testSubject.isListVisible()).toBe(true);
		});
	});

	describe("onDeleteTarget", () => {
		it("should call targetService.deleteTarget and update targets list on success", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			fixture.componentRef.setInput("targets", [mockTarget]);
			vi.spyOn(mockTargetService, "deleteTarget").mockReturnValue(of([]));

			// WHEN
			testSubject.onDeleteTarget(mockTarget.id!);
			fixture.detectChanges();

			// THEN
			expect(testSubject.targets()).toHaveLength(0);
		});
	});
});
