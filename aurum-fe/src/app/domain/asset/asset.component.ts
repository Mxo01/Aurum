import { ChangeDetectionStrategy, Component } from "@angular/core";
import { Button } from "primeng/button";
import { TableModule } from "primeng/table";

@Component({
	selector: "app-asset",
	standalone: true,
	templateUrl: "./asset.component.html",
	imports: [Button, TableModule],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetComponent {}
