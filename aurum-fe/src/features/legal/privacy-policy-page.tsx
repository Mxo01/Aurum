import { ArrowLeft } from "lucide-react";
import { Link } from "react-router";
import { Button } from "@/components/ui/button";

const SECTIONS = [
	{
		title: "1. Who we are",
		body: (
			<p>
				Aurum is a personal wealth management application available at{" "}
				<strong>aurum-networth.com</strong>. It is operated for personal use and does not constitute
				a regulated financial service.
			</p>
		)
	},
	{
		title: "2. Data we collect",
		body: (
			<>
				<p>When you use Aurum, we store:</p>
				<ul>
					<li>
						<strong>Identity data:</strong> name, email address and profile picture from your
						authentication provider.
					</li>
					<li>
						<strong>Preferences:</strong> display currency, locale and theme.
					</li>
					<li>
						<strong>Financial data:</strong> assets, value snapshots, categories and targets you
						enter.
					</li>
				</ul>
				<p>We do not collect browsing data, device fingerprints or analytics.</p>
			</>
		)
	},
	{
		title: "3. Legal basis",
		body: (
			<p>
				We process your data on the basis of <strong>contract performance</strong> (Art. 6(1)(b)
				GDPR) and <strong>legitimate interests</strong> (Art. 6(1)(f) GDPR) for application security
				and integrity.
			</p>
		)
	},
	{
		title: "4. How we use your data",
		body: (
			<>
				<ul>
					<li>To authenticate you and provide access to your account.</li>
					<li>To display your financial data and calculate analytics.</li>
					<li>To apply your currency and locale preferences.</li>
				</ul>
				<p>We do not sell, share or use your data for advertising or profiling.</p>
			</>
		)
	},
	{
		title: "5. Third-party processors",
		body: (
			<ul>
				<li>
					<strong>Auth0 (Okta)</strong> — identity and authentication, EU region.
				</li>
				<li>
					<strong>Frankfurter API</strong> — public exchange-rate data; no personal data is
					transmitted.
				</li>
				<li>
					<strong>Vercel</strong> — frontend hosting.
				</li>
				<li>
					<strong>Hetzner</strong> — backend and database hosting, EU data centre.
				</li>
			</ul>
		)
	},
	{
		title: "6. Data retention",
		body: (
			<>
				<p>
					Your data is retained while your account is active. You can delete your account at any
					time from <strong>Profile → Data &amp; privacy</strong>, which permanently removes all
					your data and the Auth0 identity.
				</p>
				<p>Internal audit logs are purged after two years, per Art. 5(1)(e) GDPR.</p>
			</>
		)
	},
	{
		title: "7. Your rights",
		body: (
			<ul>
				<li>
					<strong>Access</strong> your data directly in the application.
				</li>
				<li>
					<strong>Rectification</strong> of name, currency and locale in Profile.
				</li>
				<li>
					<strong>Erasure</strong> by deleting your account.
				</li>
				<li>
					<strong>Portability</strong> via the JSON export in Profile.
				</li>
				<li>
					<strong>Restriction</strong> by requesting suspension without deletion.
				</li>
			</ul>
		)
	},
	{
		title: "8. Cookies and local storage",
		body: (
			<>
				<p>
					Aurum uses <strong>localStorage</strong> only to remember your theme, privacy mode and
					consent.
				</p>
				<p>
					Auth0 may set strictly necessary cookies to manage your login session and prevent CSRF
					attacks. No analytics or advertising cookies are used.
				</p>
			</>
		)
	},
	{
		title: "9. Security",
		body: (
			<p>
				All data is transmitted over HTTPS. Authentication tokens are short-lived JWTs issued by
				Auth0. Database access is restricted to the application backend.
			</p>
		)
	},
	{
		title: "10. Contact",
		body: (
			<p>
				For privacy-related requests, contact the application owner or open an issue on the project
				repository.
			</p>
		)
	}
];

export function PrivacyPolicyPage() {
	return (
		<div className="mx-auto flex max-w-3xl flex-col gap-6 px-4 py-10">
			<Button
				variant="ghost"
				size="sm"
				className="self-start"
				asChild
			>
				<Link to="/">
					<ArrowLeft className="size-4" /> Back
				</Link>
			</Button>

			<div>
				<h1 className="text-2xl font-semibold tracking-tight">Privacy Policy</h1>
				<p className="text-muted-foreground mt-1 text-xs">Last updated: April 1, 2026</p>
			</div>

			<div className="flex flex-col gap-6 text-sm leading-relaxed">
				{SECTIONS.map(section => (
					<section
						key={section.title}
						className="flex flex-col gap-2"
					>
						<h2 className="text-base font-semibold">{section.title}</h2>
						<div className="text-muted-foreground [&_strong]:text-foreground [&_ul]:flex [&_ul]:list-disc [&_ul]:flex-col [&_ul]:gap-1 [&_ul]:pl-5">
							{section.body}
						</div>
					</section>
				))}
			</div>

			<footer className="border-border text-muted-foreground border-t pt-4 text-xs">
				<Link
					to="/"
					className="text-foreground underline underline-offset-2"
				>
					Back to Aurum
				</Link>
			</footer>
		</div>
	);
}
