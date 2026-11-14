import { fileURLToPath, URL } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
	plugins: [react(), tailwindcss()],
	server: { port: 4200, strictPort: true },
	preview: { port: 4200, strictPort: true },
	resolve: {
		alias: {
			"@": fileURLToPath(new URL("./src", import.meta.url))
		}
	},
	build: {
		rollupOptions: {
			output: {
				manualChunks(id) {
					if (id.includes("node_modules/recharts") || id.includes("node_modules/d3-")) {
						return "charts";
					}
					if (id.includes("node_modules/@auth0")) return "auth";
					return undefined;
				}
			}
		}
	},
	test: {
		globals: true,
		environment: "happy-dom",
		setupFiles: ["./src/test/setup.ts"],
		include: ["src/**/*.test.{ts,tsx}"],
		css: false,
		coverage: {
			provider: "v8",
			reporter: ["text", "html", "lcov"],
			include: ["src/**/*.{ts,tsx}"],
			exclude: [
				"src/**/*.test.{ts,tsx}",
				"src/test/**",
				"src/components/ui/**",
				"src/lib/api/schema.d.ts",
				"src/vite-env.d.ts",
				"src/main.tsx"
			]
		}
	}
});
