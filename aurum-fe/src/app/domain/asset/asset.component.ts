import { ChangeDetectionStrategy, Component } from "@angular/core";

@Component({
	selector: "app-asset",
	standalone: true,
	templateUrl: "./asset.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetComponent {}
