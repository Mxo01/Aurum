import { Outlet } from "react-router";
import { ConsentBanner } from "@/features/legal/consent-banner";

export function RootLayout() {
	return (
		<>
			<Outlet />
			<ConsentBanner />
		</>
	);
}
