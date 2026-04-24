import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { DealCard, DealStatus } from '../../models/deal-kanban.models';

export interface KanbanDropEvent {
  status: DealStatus;
  drop: CdkDragDrop<DealCard[]>;
}

@Component({
  selector: 'app-kanban-column',
  templateUrl: './kanban-column.component.html',
  styleUrls: ['./kanban-column.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class KanbanColumnComponent {
  @Input({ required: true }) title!: string;
  @Input({ required: true }) status!: DealStatus;
  @Input({ required: true }) connectedToIds!: string[];
  @Input({ required: true }) cards!: DealCard[];

  @Output() dropped = new EventEmitter<KanbanDropEvent>();

  get dropListId(): string {
    return `kanban-${this.status}`;
  }

  onDropped(drop: CdkDragDrop<DealCard[]>) {
    this.dropped.emit({ status: this.status, drop });
  }

  trackById(_: number, item: DealCard) {
    return item.id;
  }
}

