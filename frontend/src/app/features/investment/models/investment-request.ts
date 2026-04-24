export interface InvestmentRequest {
    id: string;
    investorId: string;
    startupId: string;
    investmentStatus: 'PENDING' | 'ACCEPTED' | 'REJECTED';
    introMessage?: string;
    ticketProposed?: number;
    investorDocUrl?: string;
    sentAt: Date;
}
