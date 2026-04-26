import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, switchMap } from 'rxjs';
import { apiOrigin } from '../../../core/api-origin';
import { InvestmentCriteria } from '../models/investment-criteria.model';

type RawInvestmentCriteria = {
  id?: string;
  name?: string;
  investorId?: string;
  sectors?: string[];
  stage?: string[];
  stages?: string[];
  minBudget?: number;
  maxBudget?: number;
  location?: string;
  active?: boolean;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

@Injectable({
  providedIn: 'root'
})
export class InvestmentCriteriaService {
  private readonly apiUrl = `${apiOrigin()}/api/invest-criteria`;

  constructor(private http: HttpClient) {}

  create(criteria: InvestmentCriteria): Observable<InvestmentCriteria> {
    return this.http.post<RawInvestmentCriteria>(`${this.apiUrl}/add`, this.toBackendPayload(criteria)).pipe(
      map((item) => this.normalizeCriteria(item))
    );
  }

  update(criteria: InvestmentCriteria): Observable<InvestmentCriteria> {
    return this.http.put<RawInvestmentCriteria>(`${this.apiUrl}/update`, this.toBackendPayload(criteria)).pipe(
      map((item) => this.normalizeCriteria(item))
    );
  }

  getById(id: string): Observable<InvestmentCriteria> {
    return this.http.get<RawInvestmentCriteria>(`${this.apiUrl}/get/${id}`).pipe(
      map((item) => this.normalizeCriteria(item))
    );
  }

  getInvestorCriteria(investorId: string): Observable<InvestmentCriteria[]> {
    return this.getAllCriteriaAdmin().pipe(
      map((items) => items.filter((item) => item.investorId === investorId))
    );
  }

  getAllCriteriaAdmin(): Observable<InvestmentCriteria[]> {
    return this.http.get<RawInvestmentCriteria[]>(`${this.apiUrl}/getAll`).pipe(
      map((items) => items.map((item) => this.normalizeCriteria(item)))
    );
  }

  deleteCriteria(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/remove/${id}`);
  }

  toggleCriteria(id: string): Observable<InvestmentCriteria> {
    return this.getById(id).pipe(
      switchMap((criteria) =>
        this.update({
          ...criteria,
          active: !this.isActive(criteria),
        })
      )
    );
  }

  isActive(criteria: InvestmentCriteria): boolean {
    return criteria.active !== false;
  }

  private normalizeCriteria(item: RawInvestmentCriteria): InvestmentCriteria {
    const sectors = Array.isArray(item.sectors) ? item.sectors : [];
    const stages = Array.isArray(item.stages)
      ? item.stages
      : Array.isArray(item.stage)
        ? item.stage
        : [];
    const active = typeof item.active === 'boolean'
      ? item.active
      : typeof item.isActive === 'boolean'
        ? item.isActive
        : true;

    return {
      id: item.id,
      name: item.name?.trim() || this.buildFallbackName(item),
      investorId: item.investorId ?? '',
      sectors,
      stages,
      minBudget: Number(item.minBudget ?? 0),
      maxBudget: Number(item.maxBudget ?? 0),
      location: item.location ?? '',
      active,
      createdAt: item.createdAt,
      updatedAt: item.updatedAt,
    };
  }

  private toBackendPayload(criteria: InvestmentCriteria): Record<string, unknown> {
    return {
      id: criteria.id,
      name: criteria.name?.trim() || null,
      investorId: criteria.investorId,
      sectors: criteria.sectors,
      stage: criteria.stages,
      minBudget: criteria.minBudget,
      maxBudget: criteria.maxBudget,
      location: criteria.location,
      active: criteria.active ?? true,
      createdAt: criteria.createdAt ?? null,
      updatedAt: criteria.updatedAt ?? null,
    };
  }

  private buildFallbackName(item: RawInvestmentCriteria): string {
    if (item.id) {
      return `Profil ${item.id.slice(-6).toUpperCase()}`;
    }
    return 'Profil investisseur';
  }
}
