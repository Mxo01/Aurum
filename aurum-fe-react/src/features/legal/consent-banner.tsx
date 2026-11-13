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
		<div className="bg-background fixed inset-x-0 bottom-16 z-50 border-t md:bottom-0">
			<div className="mx-auto flex max-w-6xl flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center">
				<p className="text-muted-foreground flex-1 text-xs leading-relaxed">
					Aurum stores your financial data to provide the service. No tracking cookies, no third
					parties.{" "}
					<Link to="/privacy" className="text-foreground underline underline-offset-2">
						Privacy Policy
					</Link>
					.
				</p>
				<Button size="sm" onClick={accept} className="shrink-0 self-start sm:self-auto">
					Accept
				</Button>
			</div>
		</div>
	);
}
