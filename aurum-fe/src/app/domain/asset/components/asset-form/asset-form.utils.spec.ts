import { AssetType, LiabilityType, PaymentFrequency } from "../../model/asset.model";
import { liabilityTypeOptions, paymentFrequencyOptions, typeOptions } from "./asset-form.utils";

describe("asset-form.utils", () => {
	describe("typeOptions", () => {
		it("should have an entry for every AssetType value", () => {
			// GIVEN / WHEN
			const values = typeOptions.map(o => o.value);

			// THEN
			expect(Object.values(AssetType).every(v => values.includes(v))).toBe(true);
		});
	});

	describe("liabilityTypeOptions", () => {
		it("should have an entry for every LiabilityType value", () => {
			// GIVEN / WHEN
			const values = liabilityTypeOptions.map(o => o.value);

			// THEN
			expect(Object.values(LiabilityType).every(v => values.includes(v))).toBe(true);
		});
	});

	describe("paymentFrequencyOptions", () => {
		it("should have an entry for every PaymentFrequency value", () => {
			// GIVEN / WHEN
			const values = paymentFrequencyOptions.map(o => o.value);

			// THEN
			expect(Object.values(PaymentFrequency).every(v => values.includes(v))).toBe(true);
		});
	});
});
