import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { apiOrigin } from '../../../core/api-origin';
import {
  CreateHoldingPayload,
  CreateMilestonePayload,
  HoldingRole,
  InvestmentHolding,
  InvestmentHoldingMilestone,
  StripeConfig,
} from '../models/investment-holding.model';

@Injectable({
  providedIn: 'root'
})
export class InvestmentHoldingService {
  private readonly baseUrl = `${apiOrigin()}/api/investments`;
  private readonly adminUrl = `${apiOrigin()}/api/admin/investments/holding`;

  constructor(private http: HttpClient) {}

  createHolding(requestId: string, payload: CreateHoldingPayload, userId: string, role: HoldingRole): Observable<InvestmentHolding> {
    return this.http.post<InvestmentHolding>(
      `${this.baseUrl}/${requestId}/holding`,
      payload,
      { headers: this.buildHeaders(userId, role) }
    );
  }

  getHolding(holdingId: string, userId: string, role: HoldingRole): Observable<InvestmentHolding> {
    return this.http.get<InvestmentHolding>(
      `${this.baseUrl}/holding/${holdingId}`,
      { headers: this.buildHeaders(userId, role) }
    );
  }

  getHoldingByRequestId(requestId: string, userId: string, role: HoldingRole): Observable<InvestmentHolding> {
    return this.http.get<InvestmentHolding>(
      `${this.baseUrl}/request/${requestId}/holding`,
      { headers: this.buildHeaders(userId, role) }
    );
  }

  confirmPayment(holdingId: string, userId: string, role: HoldingRole): Observable<InvestmentHolding> {
    return this.http.post<InvestmentHolding>(
      `${this.baseUrl}/holding/${holdingId}/confirm-payment`,
      {},
      { headers: this.buildHeaders(userId, role) }
    );
  }

  requestRelease(holdingId: string, userId: string, role: HoldingRole): Observable<InvestmentHolding> {
    return this.http.post<InvestmentHolding>(
      `${this.baseUrl}/holding/${holdingId}/request-release`,
      {},
      { headers: this.buildHeaders(userId, role) }
    );
  }

  dispute(holdingId: string, userId: string, role: HoldingRole): Observable<InvestmentHolding> {
    return this.http.post<InvestmentHolding>(
      `${this.baseUrl}/holding/${holdingId}/dispute`,
      {},
      { headers: this.buildHeaders(userId, role) }
    );
  }

  releaseFunds(holdingId: string, userId: string): Observable<InvestmentHolding> {
    return this.http.post<InvestmentHolding>(
      `${this.adminUrl}/${holdingId}/release`,
      {},
      { headers: this.buildHeaders(userId, 'ADMIN') }
    );
  }

  refund(holdingId: string, userId: string): Observable<InvestmentHolding> {
    return this.http.post<InvestmentHolding>(
      `${this.adminUrl}/${holdingId}/refund`,
      {},
      { headers: this.buildHeaders(userId, 'ADMIN') }
    );
  }

  createMilestone(holdingId: string, payload: CreateMilestonePayload, userId: string, role: HoldingRole): Observable<InvestmentHoldingMilestone> {
    return this.http.post<InvestmentHoldingMilestone>(
      `${this.baseUrl}/holding/${holdingId}/milestones`,
      payload,
      { headers: this.buildHeaders(userId, role) }
    );
  }

  validateMilestone(milestoneId: string, userId: string, role: HoldingRole): Observable<InvestmentHoldingMilestone> {
    return this.http.patch<InvestmentHoldingMilestone>(
      `${this.baseUrl}/holding/milestones/${milestoneId}/validate`,
      {},
      { headers: this.buildHeaders(userId, role) }
    );
  }

  releaseMilestone(milestoneId: string, userId: string): Observable<InvestmentHoldingMilestone> {
    return this.http.post<InvestmentHoldingMilestone>(
      `${this.adminUrl}/milestones/${milestoneId}/release`,
      {},
      { headers: this.buildHeaders(userId, 'ADMIN') }
    );
  }

  getStripeConfig(): Observable<StripeConfig> {
    return this.http.get<StripeConfig>(`${this.baseUrl}/stripe/config`);
  }

  private buildHeaders(userId: string, role: HoldingRole): HttpHeaders {
    return new HttpHeaders({
      'X-User-Id': userId,
      'X-User-Role': role,
    });
  }
}
