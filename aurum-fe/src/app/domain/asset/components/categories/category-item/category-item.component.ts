import { ChangeDetectionStrategy, Component, computed, input, output } from "@angular/core";
import { AssetCategory, AssetType } from "../../../model/asset.model";
import { Button } from "primeng/button";
import { Avatar } from "primeng/avatar";

@Component({
	selector: "app-category-item",
	imports: [Button, Avatar],
	templateUrl: "./category-item.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryItemComponent {
	category = input.required<AssetCategory>();

	edit = output<AssetCategory>();

	protected readonly AssetType = AssetType;

	protected readonly categoryColors = computed(() => ({
		bg:
			this.category().type === AssetType.ASSET
				? "color-mix(in srgb, #0ea5e9,transparent 84%)"
				: "color-mix(in srgb, #f97316,transparent 84%)",
		text: this.category().type === AssetType.ASSET ? "#7dd3fc" : "#fdba74"
	}));
}
