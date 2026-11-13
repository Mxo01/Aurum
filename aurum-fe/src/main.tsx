import "@fontsource-variable/plus-jakarta-sans";
import "@fontsource-variable/geist-mono";
import "./styles/globals.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Providers } from "./app/providers";

const container = document.getElementById("root");

if (!container) throw new Error("Root container missing");

createRoot(container).render(
	<StrictMode>
		<Providers />
	</StrictMode>
);
