export type HoldingStatus =
  | 'CREATED'
  | 'WAITING_PAYMENT'
  | 'FUNDS_HELD'
  | 'RELEASE_REQUESTED'
  | 'RELEASED'
  | 'CANCELLED'
  | 'DISPUTED'
  | 'REFUNDED';

export type MilestoneStatus =
  | 'PENDING'
  | 'VALIDATED'
  | 'RELEASED'
  | 'BLOCKED';

export type HoldingRole = 'INVESTOR' | 'STARTUP' | 'ADMIN';

export interface InvestmentHoldingMilestone {
  id: string;
  holdingId: string;
  title: string;
  amount: number;
  dueDate: string;
  status: MilestoneStatus;
  validatedByStartup: boolean;
  validatedByInvestor: boolean;
  releasedAt?: string | null;
}

export interface InvestmentHolding {
  id: string;
  investmentRequestId: string;
  investorId: string;
  startupId: string;
  amountTnd: number;
  amountEur: number;
  currencyDisplayed: string;
  stripeCurrency: string;
  status: HoldingStatus;
  stripeCheckoutSessionId?: string | null;
  stripePaymentIntentId?: string | null;
  createdAt: string;
  fundedAt?: string | null;
  releasedAt?: string | null;
  cancelledAt?: string | null;
  milestones: InvestmentHoldingMilestone[];
}

export interface CreateHoldingPayload {
  amountTnd: number;
}

export interface CheckoutSessionResponse {
  holdingId: string;
  amountTnd: number;
  amountEur: number;
  checkoutUrl: string;
}

export interface ConfirmCheckoutPayload {
  sessionId: string;
}

export interface CreateMilestonePayload {
  title: string;
  amount: number;
  dueDate: string;
}
