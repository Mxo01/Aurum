export interface ApiKeyMeta {
	id: string;
	name: string;
	createdAt: string;
	lastUsedAt: string | null;
}

export interface GeneratedKey {
	key: string;
}
