import type { TargetDTO } from "../../../shared/api/contract";

export type TargetType = TargetDTO["type"];
export const TargetType = {
	NET_WORTH: "NET_WORTH",
	MANUAL: "MANUAL"
} as const satisfies Record<TargetType, TargetType>;

export type TargetStatus = NonNullable<TargetDTO["status"]>;
export const TargetStatus = {
	COMPLETED: "COMPLETED",
	IN_PROGRESS: "IN_PROGRESS",
	NOT_REACHED: "NOT_REACHED"
} as const satisfies Record<TargetStatus, TargetStatus>;

/** `id`, progress, and status are computed by the server. */
export type Target = Omit<
	TargetDTO,
	"id" | "currentAmount" | "completionPercentage" | "isCompleted" | "status"
> & {
	id?: string;
	currentAmount?: number;
	completionPercentage?: number;
	isCompleted?: boolean;
	status?: TargetStatus;
};
