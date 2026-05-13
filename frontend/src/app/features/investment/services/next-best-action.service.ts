import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { apiOrigin } from '../../../core/api-origin';
import { NextBestAction } from '../models/next-best-action.model';

type BackendRole = 'INVESTOR' | 'STARTUP' | 'ADMIN';

@Injectable({
  providedIn: 'root'
})
export class NextBestActionService {
  private readonly baseUrl = `${apiOrigin()}/api/deals`;

  constructor(private http: HttpClient) {}

  generateAIAction(requestId: string): Observable<NextBestAction> {
    return this.http.post<NextBestAction>(
      `${this.baseUrl}/${requestId}/next-best-actions/generate-ai`,
      {},
      { headers: this.buildHeaders() }
    );
  }

  getActions(requestId: string): Observable<NextBestAction[]> {
    return this.http.get<NextBestAction[]>(
      `${this.baseUrl}/${requestId}/next-best-actions`,
      { headers: this.buildHeaders() }
    );
  }

  markDone(actionId: string): Observable<NextBestAction> {
    return this.http.patch<NextBestAction>(
      `${this.baseUrl}/next-best-actions/${actionId}/done`,
      {},
      { headers: this.buildHeaders() }
    );
  }

  ignore(actionId: string): Observable<NextBestAction> {
    return this.http.patch<NextBestAction>(
      `${this.baseUrl}/next-best-actions/${actionId}/ignore`,
      {},
      { headers: this.buildHeaders() }
    );
  }

  private buildHeaders(): HttpHeaders {
    const role = this.resolveRole();
    const userId = this.resolveUserId(role);

    return new HttpHeaders({
      'X-User-Id': userId,
      'X-User-Role': role,
    });
  }

  private resolveRole(): BackendRole {
    const raw = this.safeStorageGet('app.currentUserRole') || 'INVESTOR';
    const normalized = raw.trim().toUpperCase();
    if (normalized === 'ROLE_ADMIN' || normalized === 'ADMIN') return 'ADMIN';
    if (normalized === 'ROLE_STARTUP' || normalized === 'STARTUP') return 'STARTUP';
    return 'INVESTOR';
  }

  private resolveUserId(role: BackendRole): string {
    const stored = this.safeStorageGet('app.currentUserId');
    if (stored) return stored;

    switch (role) {
      case 'STARTUP':
        return 's-001';
      case 'ADMIN':
        return 'admin-1';
      default:
        return 'dev-investor';
    }
  }

  private safeStorageGet(key: string): string | null {
    if (typeof window === 'undefined' || !window.localStorage) {
      return null;
    }
    try {
      return window.localStorage.getItem(key);
    } catch {
      return null;
    }
  }
}
