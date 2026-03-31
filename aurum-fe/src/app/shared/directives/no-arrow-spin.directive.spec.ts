import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoArrowSpinDirective } from "./no-arrow-spin.directive";

@Component({
	template: "<div appNoArrowSpin></div>",
	imports: [NoArrowSpinDirective],
	standalone: true
})
class TestHostComponent {}

describe("NoArrowSpinDirective", () => {
	let fixture: ComponentFixture<TestHostComponent>;
	let hostEl: HTMLElement;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [TestHostComponent]
		});
		fixture = TestBed.createComponent(TestHostComponent);
		fixture.detectChanges();
		hostEl = fixture.nativeElement.querySelector("div");
	});

	describe("ArrowUp key", () => {
		it("should call preventDefault", () => {
			// GIVEN
			const event = new KeyboardEvent("keydown", {
				key: "ArrowUp",
				bubbles: true,
				cancelable: true
			});
			vi.spyOn(event, "preventDefault");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.preventDefault).toHaveBeenCalled();
		});

		it("should call stopImmediatePropagation", () => {
			// GIVEN
			const event = new KeyboardEvent("keydown", { key: "ArrowUp", bubbles: true });
			vi.spyOn(event, "stopImmediatePropagation");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.stopImmediatePropagation).toHaveBeenCalled();
		});
	});

	describe("ArrowDown key", () => {
		it("should call preventDefault", () => {
			// GIVEN
			const event = new KeyboardEvent("keydown", {
				key: "ArrowDown",
				bubbles: true,
				cancelable: true
			});
			vi.spyOn(event, "preventDefault");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.preventDefault).toHaveBeenCalled();
		});

		it("should call stopImmediatePropagation", () => {
			// GIVEN
			const event = new KeyboardEvent("keydown", { key: "ArrowDown", bubbles: true });
			vi.spyOn(event, "stopImmediatePropagation");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.stopImmediatePropagation).toHaveBeenCalled();
		});
	});

	describe("ArrowLeft key", () => {
		it("should call stopPropagation", () => {
			// GIVEN
			const event = new KeyboardEvent("keydown", { key: "ArrowLeft", bubbles: true });
			vi.spyOn(event, "stopPropagation");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.stopPropagation).toHaveBeenCalled();
		});

		it("should not call preventDefault", () => {
			// GIVEN
			const event = new KeyboardEvent("keydown", {
				key: "ArrowLeft",
				bubbles: true,
				cancelable: true
			});
			vi.spyOn(event, "preventDefault");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.preventDefault).not.toHaveBeenCalled();
		});
	});

	describe("ArrowRight key", () => {
		it("should call stopPropagation", () => {
			// GIVEN
			const event = new KeyboardEvent("keydown", { key: "ArrowRight", bubbles: true });
			vi.spyOn(event, "stopPropagation");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.stopPropagation).toHaveBeenCalled();
		});
	});

	describe("other keys", () => {
		it("should not call preventDefault for non-arrow keys", () => {
			// GIVEN
			const event = new KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true });
			vi.spyOn(event, "preventDefault");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.preventDefault).not.toHaveBeenCalled();
		});
	});

	describe("ngOnDestroy", () => {
		it("should stop handling ArrowUp after destroy", () => {
			// GIVEN
			fixture.destroy();
			const event = new KeyboardEvent("keydown", {
				key: "ArrowUp",
				bubbles: true,
				cancelable: true
			});
			vi.spyOn(event, "preventDefault");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.preventDefault).not.toHaveBeenCalled();
		});

		it("should stop handling ArrowLeft after destroy", () => {
			// GIVEN
			fixture.destroy();
			const event = new KeyboardEvent("keydown", { key: "ArrowLeft", bubbles: true });
			vi.spyOn(event, "stopPropagation");

			// WHEN
			hostEl.dispatchEvent(event);

			// THEN
			expect(event.stopPropagation).not.toHaveBeenCalled();
		});
	});
});
