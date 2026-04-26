import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { STARTUP_CATALOG_BY_ID } from '../../data/startup-catalog';
import { DealStatus, KanbanBoardResponse } from '../../models/deal-kanban.models';
import { InvestmentRequest } from '../../models/investment-request';
import {
  CreateHoldingPayload,
  CreateMilestonePayload,
  HoldingRole,
  HoldingStatus,
  InvestmentHolding,
  InvestmentHoldingMilestone,
} from '../../models/investment-holding.model';
import { DealsService } from '../../services/deals.service';
import { InvestmentHoldingService } from '../../services/investment-holding.service';
import { InvestmentRequestService } from '../../services/investment-request.service';

@Component({
  selector: 'app-investment-holding',
  templateUrl: './investment-holding.component.html',
  styleUrls: ['./investment-holding.component.css']
})
export class InvestmentHoldingComponent implements OnInit, OnDestroy {
  requestId = '';
  request: InvestmentRequest | null = null;
  holding: InvestmentHolding | null = null;
  currentDealStatus: DealStatus | null = null;

  loading = true;
  actionBusy = false;
  message = '';
  error = '';

  selectedRole: HoldingRole = 'INVESTOR';
  currentUserId = 'dev-investor';

  createHoldingForm: CreateHoldingPayload = {
    amountTnd: 0,
  };

  milestoneForm: CreateMilestonePayload = {
    title: '',
    amount: 0,
    dueDate: '',
  };

  readonly timeline: HoldingStatus[] = ['CREATED', 'WAITING_PAYMENT', 'FUNDS_HELD', 'RELEASED'];
  readonly roles: HoldingRole[] = ['INVESTOR', 'STARTUP', 'ADMIN'];
  readonly statusLabels: Record<HoldingStatus, string> = {
    CREATED: 'Cree',
    WAITING_PAYMENT: 'En attente de paiement',
    FUNDS_HELD: 'Fonds bloques',
    RELEASE_REQUESTED: 'Liberation demandee',
    RELEASED: 'Libere',
    CANCELLED: 'Annule',
    DISPUTED: 'En litige',
    REFUNDED: 'Rembourse',
  };

  private sub = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private dealService: DealsService,
    private holdingService: InvestmentHoldingService,
    private requestService: InvestmentRequestService
  ) {}

  ngOnInit(): void {
    this.requestId = this.route.snapshot.paramMap.get('requestId') ?? '';
    if (!this.requestId) {
      this.error = 'L identifiant de la demande est manquant.';
      this.loading = false;
      return;
    }

    this.loadPage();
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  get startupName(): string {
    if (!this.request?.startupId) return 'Startup inconnue';
    return STARTUP_CATALOG_BY_ID[this.request.startupId]?.name ?? this.request.startupId;
  }

  get startupSector(): string {
    if (!this.request?.startupId) return 'Inconnu';
    return STARTUP_CATALOG_BY_ID[this.request.startupId]?.sector ?? 'Inconnu';
  }

  get canCreateHolding(): boolean {
    return this.selectedRole === 'INVESTOR'
      && !!this.request
      && this.currentDealStatus === 'DUE_DILIGENCE'
      && !this.holding;
  }

  get holdingEntryStatusLabel(): string {
    switch (this.currentDealStatus) {
      case 'DISCOVERY':
        return 'Decouverte';
      case 'CONTACTED':
        return 'Contact etabli';
      case 'NEGOTIATION':
        return 'Negociation';
      case 'DUE_DILIGENCE':
        return 'Verification finale';
      case 'CLOSED':
        return 'Cloture';
      case 'REJECTED':
        return 'Rejete';
      default:
        return 'Statut non disponible';
    }
  }

  get canPay(): boolean {
    return this.selectedRole === 'INVESTOR'
      && this.currentActorMatchesInvestor
      && !!this.holding
      && this.holding.status === 'WAITING_PAYMENT';
  }

  get canOpenDispute(): boolean {
    return this.selectedRole === 'INVESTOR'
      && this.currentActorMatchesInvestor
      && !!this.holding
      && !['RELEASED', 'REFUNDED', 'CANCELLED'].includes(this.holding.status);
  }

  get canRequestRelease(): boolean {
    return this.selectedRole === 'STARTUP'
      && this.currentActorMatchesStartup
      && !!this.holding
      && this.holding.status === 'FUNDS_HELD';
  }

  get canAdminRelease(): boolean {
    return this.selectedRole === 'ADMIN'
      && !!this.holding
      && ['FUNDS_HELD', 'RELEASE_REQUESTED'].includes(this.holding.status);
  }

  get canAdminRefund(): boolean {
    return this.selectedRole === 'ADMIN'
      && !!this.holding
      && !['RELEASED', 'REFUNDED'].includes(this.holding.status);
  }

  get canCreateMilestone(): boolean {
    return !!this.holding && (this.selectedRole === 'INVESTOR' || this.selectedRole === 'ADMIN');
  }

  roleLabel(role: HoldingRole): string {
    switch (role) {
      case 'INVESTOR':
        return 'Investisseur';
      case 'STARTUP':
        return 'Startup';
      case 'ADMIN':
        return 'Administrateur';
      default:
        return role;
    }
  }

  statusLabel(status?: HoldingStatus): string {
    if (!status) return 'Inconnu';
    return this.statusLabels[status] ?? status;
  }

  onRoleChange(role: HoldingRole): void {
    this.selectedRole = role;
    if (role === 'INVESTOR') {
      this.currentUserId = this.request?.investorId ?? 'dev-investor';
    } else if (role === 'STARTUP') {
      this.currentUserId = this.request?.startupId ?? '';
    } else {
      this.currentUserId = 'admin-user';
    }
  }

  createHolding(): void {
    this.startCheckout(this.createHoldingForm.amountTnd);
  }

  payWithStripe(): void {
    if (!this.holding) {
      this.error = 'Aucun holding n est disponible pour ce paiement.';
      return;
    }
    this.startCheckout(this.holding.amountTnd);
  }

  requestRelease(): void {
    if (!this.holding) return;
    this.runHoldingAction(
      this.holdingService.requestRelease(this.holding.id, this.currentUserId, this.selectedRole),
      'Demande de liberation envoyee avec succes.'
    );
  }

  openDispute(): void {
    if (!this.holding) return;
    this.runHoldingAction(
      this.holdingService.dispute(this.holding.id, this.currentUserId, this.selectedRole),
      'Litige ouvert avec succes.'
    );
  }

  adminRelease(): void {
    if (!this.holding) return;
    this.runHoldingAction(
      this.holdingService.releaseFunds(this.holding.id, this.currentUserId),
      'Fonds liberes logiquement par l administrateur.'
    );
  }

  adminRefund(): void {
    if (!this.holding) return;
    this.runHoldingAction(
      this.holdingService.refund(this.holding.id, this.currentUserId),
      'Holding marque comme rembourse.'
    );
  }

  createMilestone(): void {
    if (!this.holding) return;
    this.resetFeedback();
    this.actionBusy = true;
    this.sub.add(
      this.holdingService.createMilestone(this.holding.id, this.milestoneForm, this.currentUserId, this.selectedRole).subscribe({
        next: () => {
          this.message = 'Jalon cree avec succes.';
          this.actionBusy = false;
          this.milestoneForm = { title: '', amount: 0, dueDate: '' };
          this.reloadHolding();
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Impossible de creer le jalon.';
          this.actionBusy = false;
        }
      })
    );
  }

  validateMilestone(milestone: InvestmentHoldingMilestone): void {
    this.resetFeedback();
    this.actionBusy = true;
    this.sub.add(
      this.holdingService.validateMilestone(milestone.id, this.currentUserId, this.selectedRole).subscribe({
        next: () => {
          this.message = 'Jalon valide avec succes.';
          this.actionBusy = false;
          this.reloadHolding();
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Impossible de valider le jalon.';
          this.actionBusy = false;
        }
      })
    );
  }

  releaseMilestone(milestone: InvestmentHoldingMilestone): void {
    this.resetFeedback();
    this.actionBusy = true;
    this.sub.add(
      this.holdingService.releaseMilestone(milestone.id, this.currentUserId).subscribe({
        next: () => {
          this.message = 'Jalon libere avec succes.';
          this.actionBusy = false;
          this.reloadHolding();
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Impossible de liberer le jalon.';
          this.actionBusy = false;
        }
      })
    );
  }

  timelineState(status: HoldingStatus): 'done' | 'active' | 'todo' {
    const actual = this.timelineIndex(this.holding?.status);
    const current = this.timelineIndex(status);
    if (actual > current) return 'done';
    if (actual === current) return 'active';
    return 'todo';
  }

  private loadPage(): void {
    this.loading = true;
    this.sub.add(
      this.requestService.getById(this.requestId).subscribe({
        next: (request) => {
          this.request = request;
          this.createHoldingForm.amountTnd = request.ticketProposed ?? 0;
          this.onRoleChange(this.selectedRole);
          this.loadDealStatus();
          this.loadExistingHolding();
        },
        error: () => {
          this.error = 'Impossible de charger la demande d investissement.';
          this.loading = false;
        }
      })
    );
  }

  private loadExistingHolding(): void {
    this.sub.add(
      this.holdingService.getHoldingByRequestId(this.requestId, this.currentUserId, this.selectedRole).subscribe({
        next: (holding) => {
          this.holding = holding;
          this.loading = false;
        },
        error: () => {
          this.holding = null;
          this.loading = false;
        }
      })
    );
  }

  private loadDealStatus(): void {
    if (!this.request?.investorId) {
      this.currentDealStatus = null;
      return;
    }

    this.sub.add(
      this.dealService.getKanban(this.request.investorId).subscribe({
        next: (board) => {
          this.currentDealStatus = this.findDealStatusForRequest(board, this.requestId);
        },
        error: () => {
          this.currentDealStatus = null;
        }
      })
    );
  }

  private reloadHolding(): void {
    if (!this.holding) return;
    this.sub.add(
      this.holdingService.getHolding(this.holding.id, this.currentUserId, this.selectedRole).subscribe({
        next: (holding) => {
          this.holding = holding;
        },
        error: () => {}
      })
    );
  }

  private startCheckout(amountTnd: number): void {
    if (!this.request) return;
    this.resetFeedback();
    this.actionBusy = true;

    this.sub.add(
      this.holdingService.createCheckoutSession(
        this.requestId,
        { amountTnd },
        this.currentUserId,
        this.selectedRole
      ).subscribe({
        next: (session) => {
          this.message = 'Redirection vers Stripe Checkout en mode test...';
          this.actionBusy = false;
          window.location.href = session.checkoutUrl;
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Impossible de demarrer Stripe Checkout.';
          this.actionBusy = false;
        }
      })
    );
  }

  private runHoldingAction(observable: ReturnType<InvestmentHoldingService['requestRelease']>, successMessage: string): void {
    this.resetFeedback();
    this.actionBusy = true;
    this.sub.add(
      observable.subscribe({
        next: (holding) => {
          this.holding = holding;
          this.message = successMessage;
          this.actionBusy = false;
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Echec de l action.';
          this.actionBusy = false;
        }
      })
    );
  }

  private timelineIndex(status?: HoldingStatus): number {
    if (!status) return -1;
    const normalized = status === 'RELEASE_REQUESTED' ? 'FUNDS_HELD' : status;
    return this.timeline.indexOf(normalized as HoldingStatus);
  }

  private resetFeedback(): void {
    this.message = '';
    this.error = '';
  }

  private findDealStatusForRequest(board: KanbanBoardResponse, requestId: string): DealStatus | null {
    const columns = board?.columns;
    if (columns) {
      for (const [status, cards] of Object.entries(columns) as [DealStatus, Array<{ requestId?: string }> | undefined][]) {
        if ((cards ?? []).some((card) => card.requestId === requestId)) {
          return status;
        }
      }
    }

    const matched = (board?.deals ?? []).find((card) => card.requestId === requestId);
    return matched?.status ?? null;
  }

  private get currentActorMatchesInvestor(): boolean {
    return this.currentUserId === (this.request?.investorId ?? this.holding?.investorId);
  }

  private get currentActorMatchesStartup(): boolean {
    return this.currentUserId === (this.request?.startupId ?? this.holding?.startupId);
  }
}
