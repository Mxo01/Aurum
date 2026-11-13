import { Construction } from "lucide-react";
import { EmptyState } from "@/components/app/empty-state";
import { PageHeader } from "@/components/app/page-header";

interface PlaceholderPageProps {
	title: string;
	description: string;
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
	return (
		<div className="flex flex-col gap-5">
			<PageHeader title={title} description={description} />
			<EmptyState
				icon={<Construction className="size-8" />}
				title="Coming in the next slice"
				description="The shell is wired. This page lands with its feature slice."
			/>
		</div>
	);
}
