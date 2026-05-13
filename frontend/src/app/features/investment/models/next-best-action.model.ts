export type NextBestActionPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type NextBestActionStatus = 'PENDING' | 'DONE' | 'IGNORED';
export type NextBestActionActorRole = 'INVESTOR' | 'STARTUP' | 'ADMIN';

export interface NextBestAction {
  id: string;
  investmentRequestId: string;
  actorRole: NextBestActionActorRole;
  title: string;
  description: string;
  priority: NextBestActionPriority;
  reason: string;
  status: NextBestActionStatus;
  aiGenerated: boolean;
  createdAt?: string;
  dueDate?: string;
}
