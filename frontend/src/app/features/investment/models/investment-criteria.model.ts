export interface InvestmentCriteria {
  id?: string;
  investorId: string;
  sectors: string[];
  stages: string[];
  minBudget: number;
  maxBudget: number;
  location: string;
}