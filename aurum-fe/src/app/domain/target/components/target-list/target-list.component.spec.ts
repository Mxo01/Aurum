import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { TargetListComponent } from "./target-list.component";
import { Target, TargetStatus, TargetType } from "../../model/target.model";
import { Currency } from "../../../profile/model/currency.model";

const buildMockTarget = (overrides: Partial<Target> = {}): Target => ({
	id: faker.string.uuid(),
	name: faker.lorem.words(2),
	targetAmount: faker.number.float({ min: 1000, max: 100000 }),
	deadline: faker.date.future().toISOString().split("T")[0],
	type: TargetType.MANUAL,
	...overrides
});

describe("TargetListComponent", () => {
	let fixture: ComponentFixture<TargetListComponent>;
	let testSubject: TargetListComponent;

	beforeEach(() => {
		TestBed.overrideComponent(TargetListComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({ imports: [TargetListComponent] });
		fixture = TestBed.createComponent(TargetListComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("isVisible", true);
		fixture.componentRef.setInput("targets", []);
		fixture.componentRef.setInput("currency", Currency.EUR);
		fixture.detectChanges();
	});

	describe("statusLabel", () => {
		it("should return Completed for COMPLETED status", () => {
			expect(testSubject.statusLabel(TargetStatus.COMPLETED)).toBe("Completed");
		});

		it("should return In Progress for IN_PROGRESS status", () => {
			expect(testSubject.statusLabel(TargetStatus.IN_PROGRESS)).toBe("In Progress");
		});

		it("should return Not Reached for NOT_REACHED status", () => {
			expect(testSubject.statusLabel(TargetStatus.NOT_REACHED)).toBe("Not Reached");
		});

		it("should return Unknown for undefined status", () => {
			expect(testSubject.statusLabel(undefined)).toBe("Unknown");
		});
	});

	describe("statusSeverity", () => {
		it("should return success for COMPLETED status", () => {
			expect(testSubject.statusSeverity(TargetStatus.COMPLETED)).toBe("success");
		});

		it("should return secondary for IN_PROGRESS status", () => {
			expect(testSubject.statusSeverity(TargetStatus.IN_PROGRESS)).toBe("secondary");
		});

		it("should return danger for NOT_REACHED status", () => {
			expect(testSubject.statusSeverity(TargetStatus.NOT_REACHED)).toBe("danger");
		});

		it("should return warn for undefined status", () => {
			expect(testSubject.statusSeverity(undefined)).toBe("warn");
		});
	});

	describe("delete output", () => {
		it("should emit the target id when delete is triggered", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			const emitted: string[] = [];
			testSubject.delete.subscribe(id => emitted.push(id));

			// WHEN
			testSubject.delete.emit(mockTarget.id!);

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0]).toBe(mockTarget.id);
		});
	});

	describe("edit output", () => {
		it("should emit the target when edit is triggered", () => {
			// GIVEN
			const mockTarget = buildMockTarget();
			const emitted: Target[] = [];
			testSubject.edit.subscribe(t => emitted.push(t));

			// WHEN
			testSubject.edit.emit(mockTarget);

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0]).toBe(mockTarget);
		});
	});
});
