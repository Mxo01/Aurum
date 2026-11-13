import { createContext, useContext, useMemo, type ReactNode } from "react";
import { createApiClient, type ApiClient } from "./client";

const ApiContext = createContext<ApiClient | null>(null);

interface ApiProviderProps {
	getToken?: () => Promise<string | undefined>;
	children: ReactNode;
}

export function ApiProvider({ getToken, children }: ApiProviderProps) {
	const client = useMemo(() => createApiClient(getToken ?? (async () => undefined)), [getToken]);

	return <ApiContext.Provider value={client}>{children}</ApiContext.Provider>;
}

export function useApi() {
	const client = useContext(ApiContext);
	if (!client) throw new Error("useApi must be used inside ApiProvider");
	return client;
}
