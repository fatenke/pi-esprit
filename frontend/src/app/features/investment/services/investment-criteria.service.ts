import { Injectable } from '@angular/core';
import { InvestmentCriteria } from '../models/investment-criteria.model';
import { Observable } from 'rxjs/internal/Observable';
import { HttpClient } from '@angular/common/http';
import { apiOrigin } from '../../../core/api-origin';

@Injectable({
  providedIn: 'root'
})
export class InvestmentCriteriaService {

  private readonly apiUrl = `${apiOrigin()}/api/invest-criteria`;

  constructor(private http: HttpClient) {}

  create(criteria: InvestmentCriteria): Observable<InvestmentCriteria> {
    return this.http.post<InvestmentCriteria>(`${this.apiUrl}/add`, criteria);
  }
  getById(id: string) {
  return this.http.get(`${this.apiUrl}/${id}`);
}
}
