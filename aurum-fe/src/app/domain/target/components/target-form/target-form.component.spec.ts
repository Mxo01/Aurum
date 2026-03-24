import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { TargetFormComponent } from "./target-form.component";
import { Target, TargetType } from "../../model/target.model";
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

describe("TargetFormComponent", () => {
	let fixture: ComponentFixture<TargetFormComponent>;
	let testSubject: TargetFormComponent;

	beforeEach(() => {
		TestBed.overrideComponent(TargetFormComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({ imports: [TargetFormComponent] });
		fixture = TestBed.createComponent(TargetFormComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("isVisible", true);
		fixture.componentRef.setInput("currency", Currency.EUR);
	});

	describe("effect — selectedTarget patching", () => {
		it("should patch the form with the selected target values when a target is provided", () => {
			// GIVEN
			const mockTarget = buildMockTarget({ type: TargetType.MANUAL });
			fixture.componentRef.setInput("selectedTarget", mockTarget);

			// WHEN
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.targetForm.controls["name"].value).toBe(mockTarget.name);
			expect(testSubject.targetForm.controls["targetAmount"].value).toBe(mockTarget.targetAmount);
		});

		it("should reset the form when selectedTarget is null", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			fixture.componentRef.setInput("selectedTarget", mockTarget);
			TestBed.flushEffects();

			// WHEN
			fixture.componentRef.setInput("selectedTarget", null);
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.targetForm.controls["name"].value).toBe("");
			expect(testSubject.targetForm.controls["targetAmount"].value).toBeNull();
		});

		it("should disable the type control when editing an existing target", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			fixture.componentRef.setInput("selectedTarget", mockTarget);

			// WHEN
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.targetForm.controls["type"].disabled).toBe(true);
		});
	});

	describe("onSubmit", () => {
		it("should emit the save output with the correctly formatted target", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedTarget", null);
			TestBed.flushEffects();
			fixture.detectChanges();
			testSubject.targetForm.setValue({
				name: "Retirement Fund",
				type: TargetType.MANUAL,
				targetAmount: 50000,
				currentAmount: 10000,
				deadline: new Date("2030-01-01")
			});
			const emitted: Target[] = [];
			testSubject.save.subscribe(t => emitted.push(t));

			// WHEN
			testSubject.onSubmit();

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0].name).toBe("Retirement Fund");
			expect(emitted[0].targetAmount).toBe(50000);
			expect(emitted[0].deadline).toBe("2030-01-01");
		});

		it("should set currentAmount to undefined for NET_WORTH type targets", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedTarget", null);
			TestBed.flushEffects();
			fixture.detectChanges();
			testSubject.targetForm.setValue({
				name: "Net Worth Goal",
				type: TargetType.NET_WORTH,
				targetAmount: 100000,
				currentAmount: 0,
				deadline: new Date("2030-01-01")
			});
			const emitted: Target[] = [];
			testSubject.save.subscribe(t => emitted.push(t));

			// WHEN
			testSubject.onSubmit();

			// THEN
			expect(emitted[0].currentAmount).toBeUndefined();
		});

		it("should not emit when the form is invalid", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedTarget", null);
			TestBed.flushEffects();
			fixture.detectChanges();
			testSubject.targetForm.patchValue({ name: "", targetAmount: null });
			const emitted: Target[] = [];
			testSubject.save.subscribe(t => emitted.push(t));

			// WHEN
			testSubject.onSubmit();

			// THEN
			expect(emitted).toHaveLength(0);
		});
	});

	describe("onHide", () => {
		it("should reset the form and clear the selectedTarget on hide", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			fixture.componentRef.setInput("selectedTarget", mockTarget);
			TestBed.flushEffects();
			fixture.detectChanges();

			// WHEN
			testSubject.onHide();
			fixture.detectChanges();

			// THEN
			expect(testSubject.targetForm.controls["name"].value).toBe("");
			expect(testSubject.selectedTarget()).toBeNull();
		});
	});
});
