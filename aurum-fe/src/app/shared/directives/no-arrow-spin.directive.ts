import { AfterViewInit, Directive, ElementRef, inject, OnDestroy } from "@angular/core";

@Directive({
	selector: "[appNoArrowSpin]",
	standalone: true
})
export class NoArrowSpinDirective implements AfterViewInit, OnDestroy {
	private readonly el = inject(ElementRef);

	private captureHandler?: (event: KeyboardEvent) => void;
	private bubbleHandler?: (event: KeyboardEvent) => void;

	ngAfterViewInit() {
		const el = this.el.nativeElement;

		this.captureHandler = (event: KeyboardEvent) => {
			if (event.key === "ArrowUp" || event.key === "ArrowDown") {
				event.preventDefault();
				event.stopImmediatePropagation();
			}
		};

		this.bubbleHandler = (event: KeyboardEvent) => {
			if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
				event.stopPropagation();
			}
		};

		// Capture phase: intercept Up/Down before the inner input receives them, preventing value spin
		el.addEventListener("keydown", this.captureHandler, true);
		// Bubble phase: after the input has moved the cursor, stop Left/Right from bubbling
		// up to the table so PrimeNG does not navigate to another cell
		el.addEventListener("keydown", this.bubbleHandler);
	}

	ngOnDestroy() {
		const el = this.el.nativeElement;
		if (this.captureHandler) el.removeEventListener("keydown", this.captureHandler, true);
		if (this.bubbleHandler) el.removeEventListener("keydown", this.bubbleHandler);
	}
}
