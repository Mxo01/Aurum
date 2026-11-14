import { useAuth0 } from "@auth0/auth0-react";
import {
	ArrowLeftRight,
	ArrowRight,
	Check,
	Download,
	EyeOff,
	Keyboard,
	LineChart,
	Lock,
	Moon,
	Sparkles,
	Sun,
	Target,
	Trash2,
	Wallet,
	Zap
} from "lucide-react";
import { Link } from "react-router";
import { CountUp } from "@/components/app/count-up";
import { Reveal } from "@/components/app/reveal";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { useThemeStore } from "@/stores/theme";
import { cn } from "@/lib/utils";
import { LandingPreview } from "./landing-preview";
import { PrivacyCarousel } from "./privacy-carousel";

const FEATURES = [
	{
		icon: LineChart,
		title: "Net worth over time",
		body: "Assets and liabilities roll up into one number, with the trend and the monthly pace behind it."
	},
	{
		icon: ArrowLeftRight,
		title: "Cash flow",
		body: "What came in, what went out, and what is left. Edit a month in place and the totals follow."
	},
	{
		icon: Wallet,
		title: "Multi-currency",
		body: "31 currencies, converted to your base currency. Your portfolio stays comparable."
	}
];

const PRIVACY = [
	{
		icon: Lock,
		title: "Auth0, EU region",
		body: "Login is handled by Auth0 in the EU region. Passwords never touch Aurum, sessions expire on their own, and you can revoke access from your account at any time."
	},
	{
		icon: EyeOff,
		title: "No trackers",
		body: "No analytics, no advertising, no third-party cookies. The only data we store is what you enter, and it is never sold, shared or used to train anything."
	},
	{
		icon: Download,
		title: "Export anytime",
		body: "Download every asset, snapshot, category and target as JSON whenever you want. Your history leaves with you, in a format you can take anywhere."
	},
	{
		icon: Trash2,
		title: "Delete anytime",
		body: "One action removes your account and all of its data from our database. No hidden copies, no waiting period, no questions asked."
	}
];

const euro = (value: number) => `€${Math.round(value).toLocaleString("en-US")}`;

export function LandingPage() {
	const { isAuthenticated, loginWithRedirect } = useAuth0();
	const { theme, toggleTheme } = useThemeStore();

	const start = () => loginWithRedirect({ appState: { returnTo: "/dashboard" } });

	return (
		<div className="landing-surface flex min-h-dvh flex-col">
			<header className="border-border/70 bg-background/80 supports-[backdrop-filter]:bg-background/60 sticky top-0 z-40 border-b pt-[env(safe-area-inset-top)] backdrop-blur-md">
				<div className="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4">
					<Link
						to="/"
						className="focus-ring flex items-center gap-2 rounded-md"
					>
						<img
							src="/favicon.svg"
							alt=""
							className="size-7"
						/>
						<span className="text-base font-semibold tracking-tight">Aurum</span>
					</Link>

					<nav
						aria-label="Main"
						className="text-muted-foreground hidden items-center gap-5 text-sm sm:flex"
					>
						<a
							href="#product"
							className="hover:text-foreground transition-colors"
						>
							Product
						</a>
						<Link
							to="/privacy"
							className="hover:text-foreground transition-colors"
						>
							Privacy
						</Link>
						<Link
							to="/privacy"
							className="hover:text-foreground transition-colors"
						>
							Policy
						</Link>
					</nav>

					<div className="ml-auto flex items-center gap-1">
						<Button
							variant="ghost"
							size="icon-sm"
							onClick={toggleTheme}
							aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
						>
							{theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
						</Button>
						{isAuthenticated ? (
							<Button
								size="sm"
								asChild
							>
								<Link to="/dashboard">Open dashboard</Link>
							</Button>
						) : (
							<>
								<Button
									variant="ghost"
									size="sm"
									onClick={() => void start()}
								>
									Sign in
								</Button>
								<Button
									size="sm"
									onClick={() => void start()}
								>
									Get started
								</Button>
							</>
						)}
					</div>
				</div>
			</header>

			<main className="flex-1">
				<section className="relative overflow-hidden px-4 pt-20 pb-14 sm:pt-28">
					<div className="relative z-10 mx-auto flex max-w-4xl flex-col items-center text-center">
						<Reveal>
							<h1 className="text-5xl leading-[0.98] tracking-tight sm:text-7xl lg:text-8xl">
								<span className="font-display block text-[1.12em] font-normal italic">
									Your{" "}
									<span className="bg-primary text-primary-foreground -skew-x-3 inline-block px-2">
										<span className="inline-block skew-x-3">net worth</span>
									</span>
									,
								</span>
								<span className="font-display text-foreground mt-2 block text-[0.62em] italic">
									finally legible.
								</span>
							</h1>
						</Reveal>

						<Reveal delay={120}>
							<p className="text-muted-foreground mt-6 max-w-xl text-base leading-relaxed sm:text-lg">
								Track assets, liabilities and cash flow in one place. Multi-currency, private by
								default, exportable anytime.
							</p>
						</Reveal>

						<Reveal delay={200}>
							<div className="mt-8 flex flex-wrap items-center justify-center gap-3">
								<Button
									size="lg"
									onClick={() => void start()}
								>
									Get started free
									<ArrowRight className="size-4" />
								</Button>
								<Button
									size="lg"
									variant="outline"
									asChild
								>
									<a href="#product">See what&apos;s inside</a>
								</Button>
							</div>
						</Reveal>

						<Reveal delay={280}>
							<ul className="text-muted-foreground mt-8 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-xs">
								<li className="flex items-center gap-1.5">
									<Check className="text-primary size-3.5" /> 31 currencies
								</li>
								<li className="flex items-center gap-1.5">
									<Check className="text-primary size-3.5" /> Unlimited categories
								</li>
								<li className="flex items-center gap-1.5">
									<Check className="text-primary size-3.5" /> No trackers
								</li>
							</ul>
						</Reveal>
					</div>
				</section>

				<section
					id="product"
					className="relative mx-auto max-w-5xl scroll-mt-24 px-4 pb-24"
				>
					<Reveal delay={80}>
						<LandingPreview />
					</Reveal>
					<p className="text-muted-foreground mt-3 text-center text-[11px]">
						Preview with sample data. Your account starts empty.
					</p>
				</section>

				<section className="border-border/70 relative overflow-hidden border-y">
					<div
						aria-hidden
						className="text-foreground/[0.04] pointer-events-none absolute top-4 right-4 z-0 text-[9rem] leading-none font-semibold tracking-tighter select-none sm:right-8 sm:text-[14rem]"
					>
						01
					</div>

					<div className="relative z-10 mx-auto grid max-w-6xl gap-10 px-4 py-20 lg:grid-cols-2 lg:items-center lg:gap-16">
						<Reveal>
							<div className="flex flex-col gap-4">
								<span className="text-muted-foreground inline-flex items-center gap-2 text-xs font-medium tracking-wide">
									<Sparkles className="text-primary size-3.5" />
									One number that matters
								</span>
								<h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
									A spreadsheet tells you where you were. Aurum tells you where you are.
								</h2>
								<p className="text-muted-foreground text-sm leading-relaxed sm:text-base">
									Every asset and liability rolls up into a single figure, updated with the values
									you record. No reconciliation, no formulas to break.
								</p>
								<ul className="mt-2 flex flex-col gap-3 text-sm">
									<li className="flex gap-3">
										<span className="bg-primary/15 text-primary mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold">
											1
										</span>
										<span>
											<strong className="font-medium">Projected net worth</strong>
											<span className="text-muted-foreground"> from your own growth rate.</span>
										</span>
									</li>
									<li className="flex gap-3">
										<span className="bg-primary/15 text-primary mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold">
											2
										</span>
										<span>
											<strong className="font-medium">Monthly pace</strong>
											<span className="text-muted-foreground">, absolute and percentage.</span>
										</span>
									</li>
									<li className="flex gap-3">
										<span className="bg-primary/15 text-primary mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold">
											3
										</span>
										<span>
											<strong className="font-medium">Debt ratio and FX impact</strong>
											<span className="text-muted-foreground"> without a second spreadsheet.</span>
										</span>
									</li>
								</ul>
							</div>
						</Reveal>

						<Reveal delay={140}>
							<div className="border-border bg-card rounded-2xl border p-6 shadow-xl">
								<div className="flex items-center justify-between">
									<span className="text-muted-foreground text-xs">Net worth</span>
									<span className="text-success bg-success/10 rounded px-1.5 py-0.5 text-[11px]">
										+9.04% / year
									</span>
								</div>
								<p className="num mt-2 text-4xl font-semibold tracking-tight">
									<CountUp
										value={570000}
										format={euro}
									/>
								</p>
								<div className="mt-5 flex flex-col gap-3">
									<div className="flex items-center justify-between text-sm">
										<span className="text-muted-foreground">Projected next year</span>
										<span className="num font-medium">€620,500.00</span>
									</div>
									<div className="flex items-center justify-between text-sm">
										<span className="text-muted-foreground">Average per month</span>
										<span className="num text-success font-medium">+€5,175.00</span>
									</div>
									<div className="flex items-center justify-between text-sm">
										<span className="text-muted-foreground">Debt to assets</span>
										<span className="num font-medium">24.8%</span>
									</div>
								</div>
								<p className="text-muted-foreground border-border mt-5 border-t pt-3 text-[11px]">
									Sample values, shown to explain the layout.
								</p>
							</div>
						</Reveal>
					</div>
				</section>

				<section className="relative overflow-hidden">
					<div
						aria-hidden
						className="text-foreground/[0.04] pointer-events-none absolute top-4 left-4 z-0 text-[9rem] leading-none font-semibold tracking-tighter select-none sm:left-8 sm:text-[14rem]"
					>
						02
					</div>

					<div className="relative z-10 mx-auto max-w-6xl px-4 py-20">
						<Reveal>
							<h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
								Three things, done properly.
							</h2>
						</Reveal>
						<Reveal delay={90}>
							<p className="text-muted-foreground mt-3 max-w-2xl text-sm sm:text-base">
								Aurum does not try to do everything. It does the three things you check every week.
							</p>
						</Reveal>
						<div className="mt-10 grid gap-5 sm:grid-cols-3">
							{FEATURES.map((feature, index) => (
								<Reveal
									key={feature.title}
									delay={index * 90}
								>
									<div
										className={cn(
											"group border-border bg-card h-full rounded-2xl border p-6 transition-all duration-300 hover:-translate-y-1 hover:shadow-xl",
											index === 1 && "sm:translate-y-6"
										)}
									>
										<div className="bg-primary/10 text-primary flex size-10 items-center justify-center rounded-xl">
											<feature.icon
												className="size-5"
												aria-hidden
											/>
										</div>
										<h3 className="mt-4 text-sm font-semibold">{feature.title}</h3>
										<p className="text-muted-foreground mt-1.5 text-sm leading-relaxed">
											{feature.body}
										</p>
									</div>
								</Reveal>
							))}
						</div>
					</div>
				</section>

				<section className="border-border/70 relative overflow-hidden border-y">
					<div
						aria-hidden
						className="text-foreground/[0.04] pointer-events-none absolute top-4 right-4 z-0 text-[9rem] leading-none font-semibold tracking-tighter select-none sm:right-8 sm:text-[14rem]"
					>
						03
					</div>
					<div className="relative z-10 mx-auto grid max-w-6xl gap-10 px-4 py-20 lg:grid-cols-2 lg:items-center">
						<Reveal>
							<div className="flex flex-col gap-4">
								<span className="text-muted-foreground inline-flex items-center gap-2 text-xs font-medium tracking-wide">
									<Zap className="text-primary size-3.5" />
									Built to be used
								</span>
								<h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
									Fewer steps between you and the number.
								</h2>
								<p className="text-muted-foreground text-sm leading-relaxed sm:text-base">
									Edit a value where you see it. Jump anywhere from the keyboard. Keep the details
									in a sheet instead of a page.
								</p>
								<div className="mt-2 grid gap-3 sm:grid-cols-2">
									{[
										{ icon: Keyboard, title: "Inline edits", body: "Click a value, type, done." },
										{ icon: Zap, title: "Keyboard first", body: "⌥D, ⌥A, ⌥C, ⌥N then A, ⌘K." },
										{
											icon: EyeOff,
											title: "Privacy mode",
											body: "Hide every amount in one click."
										},
										{
											icon: Target,
											title: "Context, not pages",
											body: "History and forms in sheets."
										}
									].map(item => (
										<div
											key={item.title}
											className="flex gap-3"
										>
											<item.icon className="text-muted-foreground mt-0.5 size-4 shrink-0" />
											<div>
												<p className="text-sm font-medium">{item.title}</p>
												<p className="text-muted-foreground text-xs">{item.body}</p>
											</div>
										</div>
									))}
								</div>
							</div>
						</Reveal>

						<Reveal delay={140}>
							<div className="border-border bg-card rounded-2xl border p-6 shadow-xl">
								<div className="flex items-center justify-between">
									<span className="text-muted-foreground flex items-center gap-2 text-xs">
										<Target className="size-3.5" /> Financial target
									</span>
									<span className="num text-xs font-medium">57%</span>
								</div>
								<p className="mt-3 text-sm font-semibold">Financial independence</p>
								<p className="text-muted-foreground num mt-0.5 text-xs">
									€570,000.00 / €1,000,000.00
								</p>
								<Progress
									value={57}
									label="Financial independence progress"
									className="mt-3"
								/>
								<div className="text-muted-foreground mt-3 flex justify-between text-[11px]">
									<span>€430,000 to go</span>
									<span>Dec 2035</span>
								</div>
								<div className="border-border mt-5 grid grid-cols-2 gap-3 border-t pt-4 text-xs">
									<div>
										<p className="text-muted-foreground">Monthly pace needed</p>
										<p className="num mt-0.5 font-medium">€4,434</p>
									</div>
									<div>
										<p className="text-muted-foreground">Status</p>
										<p className="text-success mt-0.5 font-medium">On track</p>
									</div>
								</div>
							</div>
						</Reveal>
					</div>
				</section>

				<section
					id="privacy"
					className="relative"
				>
					<div className="relative z-10 mx-auto max-w-6xl px-4 pt-20 pb-16 lg:hidden">
						<Reveal>
							<div className="relative">
								<div
									aria-hidden
									className="text-foreground/[0.04] pointer-events-none absolute -top-12 left-0 text-[9rem] leading-none font-semibold tracking-tighter select-none"
								>
									04
								</div>
								<div className="relative z-10 max-w-2xl">
									<h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
										Private by default.
									</h2>
									<p className="text-muted-foreground mt-3 text-sm sm:text-base">
										Your financial life is not a dataset to monetise. The product is the product.
									</p>
								</div>
							</div>
						</Reveal>

						<div className="mt-8 grid gap-4 sm:grid-cols-2">
							{PRIVACY.map((item, index) => (
								<Reveal
									key={item.title}
									delay={index * 80}
								>
									<div className="border-border bg-card h-full rounded-xl border p-4">
										<item.icon
											className="text-muted-foreground size-4"
											aria-hidden
										/>
										<p className="mt-3 text-sm font-medium">{item.title}</p>
										<p className="text-muted-foreground mt-1 text-xs leading-relaxed">
											{item.body}
										</p>
									</div>
								</Reveal>
							))}
						</div>
					</div>

					<PrivacyCarousel
						items={PRIVACY}
						header={
							<Reveal>
								<div className="relative">
									<div
										aria-hidden
										className="text-foreground/[0.04] pointer-events-none absolute -top-20 left-4 text-[9rem] leading-none font-semibold tracking-tighter select-none sm:left-8 sm:text-[14rem]"
									>
										04
									</div>
									<div className="relative z-10 mx-auto w-full max-w-6xl px-4">
										<div className="max-w-2xl">
											<h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
												Private by default.
											</h2>
											<p className="text-muted-foreground mt-3 text-sm sm:text-base">
												Your financial life is not a dataset to monetise. The product is the
												product.
											</p>
										</div>
									</div>
								</div>
							</Reveal>
						}
					/>
				</section>

				<section className="mx-auto max-w-6xl px-4 pt-10 pb-16">
					<Reveal>
						<div className="border-border bg-card relative overflow-hidden rounded-3xl border px-6 py-12 text-center shadow-2xl sm:px-10 sm:py-16">
							<div className="relative z-10">
								<h2 className="text-3xl font-semibold tracking-tight sm:text-5xl">
									Start with one asset.
									<br />
									<span className="text-muted-foreground">Add the rest later.</span>
								</h2>
								<p className="text-muted-foreground mx-auto mt-4 max-w-md text-sm sm:text-base">
									Free to use. Your data stays yours. No subscriptions, no tracking.
								</p>
								<Button
									size="lg"
									className="mt-8"
									onClick={() => void start()}
								>
									Get started free
									<ArrowRight className="size-4" />
								</Button>
								<ul className="text-muted-foreground mt-8 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-xs">
									<li className="flex items-center gap-1.5">
										<Check className="text-primary size-3.5" /> Free forever
									</li>
									<li className="flex items-center gap-1.5">
										<Check className="text-primary size-3.5" /> No credit card
									</li>
									<li className="flex items-center gap-1.5">
										<Check className="text-primary size-3.5" /> Export anytime
									</li>
								</ul>
							</div>
						</div>
					</Reveal>
				</section>
			</main>

			<footer className="border-border/70 border-t">
				<div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-6 text-xs">
					<div className="text-muted-foreground flex items-center gap-2">
						<img
							src="/favicon.svg"
							alt=""
							className="size-4"
						/>
						<span>© {new Date().getFullYear()} Aurum</span>
					</div>
					<div className="text-muted-foreground flex items-center gap-4">
						<Link
							to="/privacy"
							className="hover:text-foreground transition-colors"
						>
							Privacy Policy
						</Link>
						<a
							href="#product"
							className="hover:text-foreground transition-colors"
						>
							Product
						</a>
					</div>
				</div>
			</footer>
		</div>
	);
}
