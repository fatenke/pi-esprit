import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiOrigin } from '../../../core/api-origin';
import { KanbanBoardResponse, MoveDealPayload } from '../models/deal-kanban.models';

@Injectable({
  providedIn: 'root'
})
export class DealsService {

  private readonly apiUrl = `${apiOrigin()}/api/deals`;

  constructor(private http: HttpClient) {}

  getKanban(investorId: string): Observable<KanbanBoardResponse> {
    return this.http.get<KanbanBoardResponse>(`${this.apiUrl}/board/${investorId}`);
  }

  moveCard(data: MoveDealPayload): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/move`, data);
  }
}
