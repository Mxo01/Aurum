import { Link } from "react-router";

export function Brand({ to = "/dashboard" }: { to?: string }) {
	return (
		<Link
			to={to}
			className="focus-ring flex items-center gap-2 rounded-md"
			aria-label="Aurum home"
		>
			<img
				src="/favicon.svg"
				alt=""
				className="size-7"
			/>
			<span className="text-base font-semibold tracking-tight">Aurum</span>
		</Link>
	);
}
