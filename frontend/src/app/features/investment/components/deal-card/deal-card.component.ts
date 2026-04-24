import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Router } from '@angular/router';
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

  constructor(private router: Router) {}

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
}
