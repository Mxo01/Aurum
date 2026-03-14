import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Asset, AssetCategory } from "./model/asset.model";
import { forkJoin, switchMap, tap } from "rxjs";
import { MessageService } from "primeng/api";

@Injectable({
	providedIn: "root"
})
export class AssetService {
	private readonly messageService = inject(MessageService);
	private http = inject(HttpClient);

	private readonly assetsUrl = `${environment.apiUrl}/assets`;
	private readonly assetCategoriesUrl = `${environment.apiUrl}/categories`;

	getUserAssetCategories() {
		return this.http.get<AssetCategory[]>(`${this.assetCategoriesUrl}/user`);
	}

	getAllAssetCategories() {
		return this.http.get<AssetCategory[]>(this.assetCategoriesUrl);
	}

	saveCategory(category: AssetCategory) {
		return category.id
			? this.http.put<AssetCategory>(`${this.assetCategoriesUrl}/${category.id}`, category).pipe(
					switchMap(() =>
						forkJoin({ categories: this.getUserAssetCategories(), assets: this.getAssets() })
					),
					tap({
						next: () => {
							this.messageService.add({
								severity: "success",
								summary: "Success",
								detail: "Category updated successfully"
							});
						},
						error: () => {
							this.messageService.add({
								severity: "error",
								summary: "Error",
								detail: "Failed to update category"
							});
						}
					})
				)
			: this.http.post<AssetCategory>(this.assetCategoriesUrl, category).pipe(
					switchMap(() =>
						forkJoin({ categories: this.getUserAssetCategories(), assets: this.getAssets() })
					),
					tap({
						next: () => {
							this.messageService.add({
								severity: "success",
								summary: "Success",
								detail: "Category created successfully"
							});
						},
						error: () => {
							this.messageService.add({
								severity: "error",
								summary: "Error",
								detail: "Failed to create category"
							});
						}
					})
				);
	}

	deleteCategory(id: string) {
		return this.http.delete<AssetCategory[]>(`${this.assetCategoriesUrl}/${id}`).pipe(
			switchMap(() =>
				forkJoin({ categories: this.getUserAssetCategories(), assets: this.getAssets() })
			),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Category deleted successfully"
					});
				},
				error: () => {
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to delete category"
					});
				}
			})
		);
	}

	getAssets() {
		return this.http.get<Asset[]>(this.assetsUrl);
	}

	saveAsset(asset: Asset) {
		return asset.id
			? this.http.put<Asset>(`${this.assetsUrl}/${asset.id}`, asset).pipe(
					switchMap(() => this.getAssets()),
					tap({
						next: () => {
							this.messageService.add({
								severity: "success",
								summary: "Success",
								detail: "Asset updated successfully"
							});
						},
						error: () => {
							this.messageService.add({
								severity: "error",
								summary: "Error",
								detail: "Failed to update asset"
							});
						}
					})
				)
			: this.http.post<Asset>(this.assetsUrl, asset).pipe(
					switchMap(() => this.getAssets()),
					tap({
						next: () => {
							this.messageService.add({
								severity: "success",
								summary: "Success",
								detail: "Asset created successfully"
							});
						},
						error: () => {
							this.messageService.add({
								severity: "error",
								summary: "Error",
								detail: "Failed to create asset"
							});
						}
					})
				);
	}

	deleteAssetPermanently(id: string) {
		return this.http.delete<void>(`${this.assetsUrl}/${id}`).pipe(
			switchMap(() => this.getAssets()),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Asset deleted permanently"
					});
				},
				error: () => {
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to delete asset permanently"
					});
				}
			})
		);
	}
}
