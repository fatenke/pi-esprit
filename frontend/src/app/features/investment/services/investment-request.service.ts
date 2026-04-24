import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { apiOrigin } from '../../../core/api-origin';
import { InvestmentRequest } from '../models/investment-request';

@Injectable({
  providedIn: 'root'
})
export class InvestmentRequestService {

  private readonly apiUrl = `${apiOrigin()}/api/invest-request`;

  constructor(private http: HttpClient) {}

  create(form: FormData, investorId?: string): Observable<InvestmentRequest> {
    const headers = investorId
      ? new HttpHeaders({ 'X-Investor-Id': investorId })
      : undefined;

    return this.http.post<InvestmentRequest>(
      `${this.apiUrl}/add`,
      form,
      { headers }
    );
  }

  getById(id: string): Observable<InvestmentRequest> {
    return this.http.get<InvestmentRequest>(
      `${this.apiUrl}/get/${id}`
    );
  }

  getByInvestor(id: string): Observable<InvestmentRequest[]> {
    return this.http.get<InvestmentRequest[]>(
      `${this.apiUrl}/investor/${id}`
    );
  }

  getByStartup(startupId: string): Observable<InvestmentRequest[]> {
    return this.http.get<InvestmentRequest[]>(
      `${this.apiUrl}/startup/${startupId}`
    );
  }

  acceptRequest(id: string): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/${id}/accept`,
      {}
    );
  }

  rejectRequest(id: string): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/${id}/reject`,
      {}
    );
  }

  updateRequest(id: string, data: InvestmentRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/update`, data);
  }

  downloadFile(id: string) {
    return this.http.get(
      `${this.apiUrl}/files/${id}`,
      { responseType: 'blob' }
    );
  }
}
