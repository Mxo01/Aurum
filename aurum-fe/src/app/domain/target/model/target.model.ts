export enum TargetType {
	NET_WORTH = "NET_WORTH",
	MANUAL = "MANUAL"
}

export enum TargetStatus {
	COMPLETED = "COMPLETED",
	IN_PROGRESS = "IN_PROGRESS",
	NOT_REACHED = "NOT_REACHED"
}

export interface Target {
	id?: string;
	name: string;
	targetAmount: number;
	currentAmount?: number;
	completionPercentage?: number;
	deadline: string;
	isCompleted?: boolean;
	type: TargetType;
	status?: TargetStatus;
}
