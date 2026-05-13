import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { NextBestAction } from '../../models/next-best-action.model';

@Component({
  selector: 'app-next-best-action-card',
  templateUrl: './next-best-action-card.component.html',
  styleUrls: ['./next-best-action-card.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NextBestActionCardComponent {
  @Input({ required: true }) action!: NextBestAction;
  @Input() busy = false;

  @Output() done = new EventEmitter<string>();
  @Output() ignore = new EventEmitter<string>();

  get priorityLabel(): string {
    switch (this.action.priority) {
      case 'LOW':
        return 'Faible';
      case 'MEDIUM':
        return 'Moyenne';
      case 'HIGH':
        return 'Haute';
      case 'URGENT':
        return 'Urgente';
      default:
        return this.action.priority;
    }
  }

  get actorLabel(): string {
    switch (this.action.actorRole) {
      case 'INVESTOR':
        return 'Investisseur';
      case 'STARTUP':
        return 'Startup';
      case 'ADMIN':
        return 'Admin';
      default:
        return this.action.actorRole;
    }
  }

  get statusLabel(): string {
    switch (this.action.status) {
      case 'DONE':
        return 'Traitee';
      case 'IGNORED':
        return 'Ignoree';
      default:
        return 'En attente';
    }
  }

  markDone(): void {
    this.done.emit(this.action.id);
  }

  markIgnore(): void {
    this.ignore.emit(this.action.id);
  }
}
