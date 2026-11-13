import { defineConfig } from "@playwright/test";

const isCI = Boolean(process.env.CI);
const channel = isCI ? {} : { channel: "chrome" as const };

export default defineConfig({
	testDir: "./e2e",
	fullyParallel: true,
	timeout: 30_000,
	retries: isCI ? 2 : 0,
	reporter: [["list"], ["html", { open: "never" }]],
	use: {
		baseURL: "http://localhost:4173",
		trace: "on-first-retry",
		...channel
	},
	projects: [
		{ name: "desktop", use: { viewport: { width: 1440, height: 900 } } },
		{
			name: "mobile",
			use: {
				viewport: { width: 390, height: 844 },
				isMobile: true,
				hasTouch: true,
				deviceScaleFactor: 2
			}
		}
	],
	webServer: {
		command: "npm run build && npm run preview -- --port 4173 --strictPort",
		url: "http://localhost:4173",
		reuseExistingServer: !isCI,
		timeout: 120_000,
		env: {
			VITE_API_URL: "http://api.test"
		}
	}
});
