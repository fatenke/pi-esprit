export interface InvestmentCriteria {
  id?: string;
  name?: string;
  investorId: string;
  sectors: string[];
  stages: string[];
  minBudget: number;
  maxBudget: number;
  location: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
