import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { DataRoomApiService } from '../../../data-room/services/data-room-api.service';
import { DealCard } from '../../models/deal-kanban.models';
import { NextBestAction } from '../../models/next-best-action.model';
import { NextBestActionService } from '../../services/next-best-action.service';

@Component({
  selector: 'app-deal-card',
  templateUrl: './deal-card.component.html',
  styleUrls: ['./deal-card.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DealCardComponent {
  @Input({ required: true }) card!: DealCard;
  expanded = false;
  actions: NextBestAction[] = [];
  actionsLoading = false;
  generating = false;
  actionUpdateLoading = false;
  actionsError: string | null = null;

  constructor(
    private router: Router,
    private dataRoomApi: DataRoomApiService,
    private nextBestActionService: NextBestActionService
  ) {}

  get alertClass(): string {
    switch (this.card.alertLevel) {
      case 'RED':
        return 'alert-red';
      case 'YELLOW':
        return 'alert-yellow';
      default:
        return 'alert-none';
    }
  }

  get canOpenHolding(): boolean {
    return !!this.card.requestId && this.card.status === 'DUE_DILIGENCE';
  }

  get canOpenDataRoom(): boolean {
    return !!this.card.id && this.card.status === 'NEGOTIATION';
  }

  get statusLabel(): string {
    switch (this.card.status) {
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
        return this.card.status;
    }
  }

  toggleDetails(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.expanded = !this.expanded;
    if (this.expanded) {
      this.loadActions();
    }
  }

  openHolding(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (!this.card.requestId) return;
    this.router.navigate(['/investment/holding/request', this.card.requestId]);
  }

  openDataRoom(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (!this.card.id) return;

    this.dataRoomApi.ensureRoomForDeal(String(this.card.id)).subscribe({
      next: (room) => {
        if (!room.id) {
          alert('Impossible d ouvrir la data room pour ce deal.');
          return;
        }
        this.router.navigate(['/investment/data-room', room.id]);
      },
      error: () => {
        alert('Impossible d ouvrir la data room pour ce deal.');
      }
    });
  }

  generateRecommendation(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (!this.card.requestId || this.generating) return;

    this.generating = true;
    this.actionsError = null;

    this.nextBestActionService.generateAIAction(String(this.card.requestId)).subscribe({
      next: (action) => {
        this.actions = [action, ...this.actions.filter((item) => item.id !== action.id)];
        this.generating = false;
      },
      error: () => {
        this.actionsError = 'Impossible de generer la recommandation IA.';
        this.generating = false;
      }
    });
  }

  markDone(actionId: string): void {
    if (this.actionUpdateLoading) return;
    this.actionUpdateLoading = true;
    this.actionsError = null;

    this.nextBestActionService.markDone(actionId).subscribe({
      next: (updated) => {
        this.replaceAction(updated);
        this.actionUpdateLoading = false;
      },
      error: () => {
        this.actionsError = 'Impossible de mettre a jour cette action.';
        this.actionUpdateLoading = false;
      }
    });
  }

  ignoreAction(actionId: string): void {
    if (this.actionUpdateLoading) return;
    this.actionUpdateLoading = true;
    this.actionsError = null;

    this.nextBestActionService.ignore(actionId).subscribe({
      next: (updated) => {
        this.replaceAction(updated);
        this.actionUpdateLoading = false;
      },
      error: () => {
        this.actionsError = 'Impossible de mettre a jour cette action.';
        this.actionUpdateLoading = false;
      }
    });
  }

  private loadActions(): void {
    if (!this.card.requestId) {
      this.actions = [];
      return;
    }

    this.actionsLoading = true;
    this.actionsError = null;

    this.nextBestActionService.getActions(String(this.card.requestId)).subscribe({
      next: (actions) => {
        this.actions = actions ?? [];
        this.actionsLoading = false;
      },
      error: () => {
        this.actions = [];
        this.actionsError = 'Impossible de charger les recommandations pour ce deal.';
        this.actionsLoading = false;
      }
    });
  }

  private replaceAction(updated: NextBestAction): void {
    this.actions = this.actions.map((item) => item.id === updated.id ? updated : item);
  }
}
