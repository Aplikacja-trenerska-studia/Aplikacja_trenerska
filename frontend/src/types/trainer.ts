export type GroupMember = { userId: number; displayName: string; email: string; status: 'PENDING' | 'ACCEPTED'; invitedAt: string; acceptedAt: string | null }
export type TrainingGroup = { id: number; name: string; createdAt: string; members: GroupMember[] }
export type GroupInvitation = { groupId: number; groupName: string; trainerName: string; status: 'PENDING' | 'ACCEPTED'; invitedAt: string; acceptedAt: string | null }
export type PlanAssignment = { id: number; planId: number | null; planName: string; trainerName: string; groupName: string; userId: number; displayName: string; assignedAt: string }
