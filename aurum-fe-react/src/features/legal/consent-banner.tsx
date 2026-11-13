import { useState } from "react";
import { Link } from "react-router";
import { Button } from "@/components/ui/button";
import { readConsent, storeConsent } from "./consent";

export function ConsentBanner() {
	const [visible, setVisible] = useState(() => !readConsent());

	if (!visible) return null;

	const accept = () => {
		storeConsent();
		setVisible(false);
	};

	return (
		<div className="fixed inset-x-4 bottom-20 z-50 mx-auto max-w-2xl md:bottom-4">
			<div className="bg-card border-border flex flex-col gap-3 rounded-xl border p-4 shadow-lg sm:flex-row sm:items-center">
				<p className="text-muted-foreground flex-1 text-xs leading-relaxed">
					Aurum stores your financial data to provide the service. No tracking cookies, no third
					parties.{" "}
					<Link to="/privacy" className="text-foreground underline underline-offset-2">
						Privacy Policy
					</Link>
					.
				</p>
				<Button size="sm" onClick={accept} className="shrink-0">
					Accept
				</Button>
			</div>
		</div>
	);
}
