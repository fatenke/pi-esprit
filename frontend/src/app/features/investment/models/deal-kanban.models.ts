export type DealStatus =
  | 'DISCOVERY'
  | 'CONTACTED'
  | 'NEGOTIATION'
  | 'DUE_DILIGENCE'
  | 'CLOSED'
  | 'REJECTED';

export type AlertLevel = 'NONE' | 'YELLOW' | 'RED';

export interface DealCard {
  id: string | number;
  startupName?: string;
  startupId?: string;
  startupSector?: string;
  requestId?: string;
  status: DealStatus;
  ticketProposed?: number | null;
  lastStatusChangeAt?: string;
  daysInStatus: number;
  alertLevel?: AlertLevel;
  /**
   * Optional order within a status column. If backend provides it, we sort by it.
   */
  columnOrder?: number;
}

export interface KanbanBoardResponse {
  /**
   * Flexible shape to match backend implementations:
   * - either `{ columns: { [status]: DealCard[] } }`
   * - or `{ [status]: DealCard[] }`
   * - or `{ deals: DealCard[] }`
   */
  columns?: Partial<Record<DealStatus, DealCard[]>>;
  deals?: DealCard[];
  [key: string]: unknown;
}

export interface MoveDealPayload {
  dealId: string | number;
  newStatus: DealStatus;
  newColumnOrder: number;
}
