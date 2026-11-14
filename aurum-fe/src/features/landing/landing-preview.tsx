import { ArrowDownRight, ArrowUpRight, Moon, Sun } from "lucide-react";
import { CountUp } from "@/components/app/count-up";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useThemeStore, type Theme } from "@/stores/theme";
import { cn } from "@/lib/utils";

const euro = new Intl.NumberFormat("en-US", { style: "currency", currency: "EUR" });

const SAMPLE_ASSETS = [
	{ name: "Apartment Berlin", value: "€430,000.00", change: "+4.37%", positive: true },
	{ name: "Global Equity ETF", value: "€128,450.00", change: "+7.67%", positive: true },
	{ name: "Pension Fund", value: "€86,400.00", change: "+1.02%", positive: true },
	{ name: "Mortgage", value: "€188,400.00", change: "−2.01%", positive: false }
];

export function LandingPreview() {
	const { theme, setTheme } = useThemeStore();

	return (
		<div className="border-border bg-card overflow-hidden rounded-xl border shadow-sm">
			<div className="border-border flex items-center justify-between gap-3 border-b px-4 py-2.5">
				<div className="flex items-center gap-2">
					<img
						src="/favicon.svg"
						alt=""
						className="size-4"
					/>
					<span className="text-xs font-semibold">Aurum</span>
					<span className="text-muted-foreground hidden text-[11px] sm:inline">Sample data</span>
				</div>
				<Tabs
					value={theme}
					onValueChange={value => setTheme(value as Theme)}
				>
					<TabsList aria-label="Preview theme">
						<TabsTrigger
							value="light"
							aria-controls={undefined}
						>
							<Sun className="size-3" /> Light
						</TabsTrigger>
						<TabsTrigger
							value="dark"
							aria-controls={undefined}
						>
							<Moon className="size-3" /> Dark
						</TabsTrigger>
					</TabsList>
				</Tabs>
			</div>

			<div className="grid gap-4 p-4 sm:p-5 lg:grid-cols-[1.7fr_1fr]">
				<div className="flex flex-col gap-4">
					<div className="flex flex-wrap items-end justify-between gap-3">
						<div className="flex flex-col gap-1">
							<span className="text-muted-foreground text-[11px] font-medium">Net worth</span>
							<div className="flex items-baseline gap-2">
								<span className="num text-2xl font-semibold sm:text-3xl">
									<CountUp
										value={570000}
										format={value => euro.format(value)}
									/>
								</span>
								<span className="text-success bg-success/10 inline-flex items-center gap-0.5 rounded-md px-1.5 py-0.5 text-[11px] font-medium">
									<ArrowUpRight className="size-3" /> 0.61%
								</span>
							</div>
							<span className="text-muted-foreground text-[11px]">+€5,175.00 avg / month</span>
						</div>
						<div className="bg-muted flex items-center gap-0.5 rounded-md p-0.5">
							{["1M", "6M", "1Y"].map(period => (
								<span
									key={period}
									className={cn(
										"rounded px-2 py-0.5 text-[11px] font-medium",
										period === "1Y" ? "bg-card text-foreground shadow-sm" : "text-muted-foreground"
									)}
								>
									{period}
								</span>
							))}
						</div>
					</div>

					<div className="relative h-40 w-full sm:h-48">
						<svg
							viewBox="0 0 560 180"
							className="size-full"
							preserveAspectRatio="none"
							aria-hidden
						>
							<g
								className="text-border"
								stroke="currentColor"
								strokeWidth="1"
							>
								<line
									x1="0"
									y1="45"
									x2="560"
									y2="45"
								/>
								<line
									x1="0"
									y1="90"
									x2="560"
									y2="90"
								/>
								<line
									x1="0"
									y1="135"
									x2="560"
									y2="135"
								/>
							</g>
							<defs>
								<linearGradient
									id="preview-area"
									x1="0"
									y1="0"
									x2="0"
									y2="1"
								>
									<stop
										offset="0%"
										stopColor="var(--primary)"
										stopOpacity={0.35}
									/>
									<stop
										offset="55%"
										stopColor="var(--primary)"
										stopOpacity={0.12}
									/>
									<stop
										offset="100%"
										stopColor="var(--primary)"
										stopOpacity={0}
									/>
								</linearGradient>
							</defs>
							<path
								d="M0,148 C40,145 70,140 110,132 C150,124 180,122 220,112 C260,102 290,98 330,86 C370,74 400,68 440,56 C480,44 510,40 560,28 L560,180 L0,180 Z"
								fill="url(#preview-area)"
							/>
							<path
								d="M0,148 C40,145 70,140 110,132 C150,124 180,122 220,112 C260,102 290,98 330,86 C370,74 400,68 440,56 C480,44 510,40 560,28"
								className="stroke-primary"
								fill="none"
								strokeWidth="2.5"
								strokeLinecap="round"
							/>
							<circle
								cx="560"
								cy="28"
								r="4"
								className="fill-primary"
							/>
						</svg>
					</div>

					<div className="text-muted-foreground flex justify-between text-[10px]">
						{["Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun"].map(month => (
							<span key={month}>{month}</span>
						))}
					</div>
				</div>

				<div className="flex flex-col gap-3">
					<div className="border-border grid grid-cols-2 gap-3 rounded-lg border p-3">
						<div className="flex flex-col gap-0.5">
							<span className="text-muted-foreground text-[10px]">Projected</span>
							<span className="num text-sm font-semibold">€620,500</span>
						</div>
						<div className="flex flex-col gap-0.5">
							<span className="text-muted-foreground text-[10px]">Liabilities</span>
							<span className="num text-sm font-semibold">€188,400</span>
						</div>
						<div className="flex flex-col gap-0.5">
							<span className="text-muted-foreground text-[10px]">Cash flow</span>
							<span className="num text-success text-sm font-semibold">+€2,174</span>
						</div>
						<div className="flex flex-col gap-0.5">
							<span className="text-muted-foreground text-[10px]">Debt ratio</span>
							<span className="num text-sm font-semibold">24.8%</span>
						</div>
					</div>

					<div className="border-border flex flex-col gap-2 rounded-lg border p-3">
						{SAMPLE_ASSETS.map(asset => (
							<div
								key={asset.name}
								className="flex items-center justify-between gap-2"
							>
								<span className="text-muted-foreground truncate text-[11px]">{asset.name}</span>
								<div className="flex items-center gap-2">
									<span className="num text-[11px] font-medium">{asset.value}</span>
									<span
										className={cn(
											"num inline-flex items-center gap-0.5 text-[10px]",
											asset.positive ? "text-success" : "text-danger"
										)}
									>
										{asset.positive ? (
											<ArrowUpRight className="size-3" />
										) : (
											<ArrowDownRight className="size-3" />
										)}
										{asset.change}
									</span>
								</div>
							</div>
						))}
					</div>
				</div>
			</div>
		</div>
	);
}
