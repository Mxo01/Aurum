import { ChangeDetectionStrategy, Component, computed, input, output } from "@angular/core";
import { Button } from "primeng/button";
import { AssetCategory, AssetType } from "../../asset/model/asset.model";

@Component({
	selector: "app-category-item",
	imports: [Button],
	templateUrl: "./category-item.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryItemComponent {
	category = input.required<AssetCategory>();

	edit = output<AssetCategory>();

	readonly AssetType = AssetType;

	readonly categoryColors = computed(() => ({
		bg:
			this.category().type === AssetType.ASSET
				? "color-mix(in srgb, #0ea5e9,transparent 84%)"
				: "color-mix(in srgb, #f97316,transparent 84%)",
		text: this.category().type === AssetType.ASSET ? "#7dd3fc" : "#fdba74"
	}));
}
