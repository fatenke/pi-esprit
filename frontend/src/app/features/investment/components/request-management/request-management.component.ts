import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { InvestmentRequestService } from '../../services/investment-request.service';
import { InvestmentRequest } from '../../models/investment-request';

type RequestStatusFilter = 'ALL' | 'PENDING' | 'ACCEPTED' | 'REJECTED';

@Component({
  selector: 'app-request-management',
  templateUrl: './request-management.component.html',
  styleUrl: './request-management.component.css'
})
export class RequestManagementComponent implements OnInit {
  requests: InvestmentRequest[] = [];
  selectedStatus: RequestStatusFilter = 'ALL';
  query = '';

  readonly STATUS_LABELS: Record<string, string> = {
    PENDING: 'En attente',
    ACCEPTED: 'Acceptee',
    REJECTED: 'Refusee',
  };

  readonly statusFilters: { value: RequestStatusFilter; label: string }[] = [
    { value: 'ALL', label: 'Toutes' },
    { value: 'PENDING', label: 'En attente' },
    { value: 'ACCEPTED', label: 'Acceptees' },
    { value: 'REJECTED', label: 'Refusees' },
  ];

  investorId = 'dev-investor';

  constructor(
    private requestService: InvestmentRequestService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.requestService.getByInvestor(this.investorId).subscribe({
      next: (data) => {
        this.requests = this.sortRequests(data || []);
      },
      error: (err) => {
        console.error(err);
        this.requests = [];
      }
    });
  }

  get filteredRequests(): InvestmentRequest[] {
    const query = this.query.trim().toLowerCase();

    return this.requests.filter((req) => {
      const statusMatch =
        this.selectedStatus === 'ALL' || req.investmentStatus === this.selectedStatus;

      const textMatch =
        !query ||
        req.investorId.toLowerCase().includes(query) ||
        req.startupId.toLowerCase().includes(query) ||
        (req.introMessage ?? '').toLowerCase().includes(query);

      return statusMatch && textMatch;
    });
  }

  get pendingCount(): number {
    return this.requests.filter((req) => req.investmentStatus === 'PENDING').length;
  }

  get acceptedCount(): number {
    return this.requests.filter((req) => req.investmentStatus === 'ACCEPTED').length;
  }

  get rejectedCount(): number {
    return this.requests.filter((req) => req.investmentStatus === 'REJECTED').length;
  }

  accept(id: string): void {
    this.requestService.acceptRequest(id).subscribe({
      next: () => {
        this.updateStatus(id, 'ACCEPTED');
      },
      error: (err) => console.error(err)
    });
  }

  reject(id: string): void {
    this.requestService.rejectRequest(id).subscribe({
      next: () => {
        this.updateStatus(id, 'REJECTED');
      },
      error: (err) => console.error(err)
    });
  }

  private updateStatus(id: string, status: 'ACCEPTED' | 'REJECTED') {
    const req = this.requests.find((r) => r.id === id);
    if (req) {
      req.investmentStatus = status;
    }
    this.requests = this.sortRequests(this.requests);
  }

  private sortRequests(requests: InvestmentRequest[]): InvestmentRequest[] {
    return [...requests].sort((a, b) => {
      const at = new Date(a.sentAt).getTime();
      const bt = new Date(b.sentAt).getTime();
      return bt - at;
    });
  }

  setStatusFilter(status: RequestStatusFilter): void {
    this.selectedStatus = status;
  }

  clearFilters(): void {
    this.selectedStatus = 'ALL';
    this.query = '';
  }

  download(req: InvestmentRequest): void {
    if (req.investorDocUrl) {
      window.open(req.investorDocUrl, '_blank', 'noopener,noreferrer');
    }
  }

  update(id: string): void {
    this.router.navigate(['/investment/edit-request', id]);
  }

  viewInKanban(_: string): void {
    this.router.navigate(['/investment/kanban']);
  }

  getStatusLabel(s: string): string {
    return this.STATUS_LABELS[s] ?? s;
  }

  getInitials(name: string): string {
    return name.split(' ').slice(0, 2).map((w) => w[0]).join('').toUpperCase();
  }

  trackById(_: number, req: InvestmentRequest): string {
    return req.id;
  }
}
