import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { DataRoomApiService } from '../../../data-room/services/data-room-api.service';
import { DealCard } from '../../models/deal-kanban.models';

@Component({
  selector: 'app-deal-card',
  templateUrl: './deal-card.component.html',
  styleUrls: ['./deal-card.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DealCardComponent {
  @Input({ required: true }) card!: DealCard;
  expanded = false;

  constructor(
    private router: Router,
    private dataRoomApi: DataRoomApiService
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
}
