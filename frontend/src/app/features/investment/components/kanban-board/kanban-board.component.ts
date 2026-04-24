import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Component, OnInit } from '@angular/core';
import { DealsService } from '../../services/deals.service';
import { DealCard, DealStatus, KanbanBoardResponse, MoveDealPayload } from '../../models/deal-kanban.models';
import { STARTUP_CATALOG_BY_ID } from '../../data/startup-catalog';
import { KanbanDropEvent } from '../kanban-column/kanban-column.component';

@Component({
  selector: 'app-kanban-board',
  templateUrl: './kanban-board.component.html',
  styleUrls: ['./kanban-board.component.css']
})
export class KanbanBoardComponent implements OnInit {
  investorId = 'dev-investor';

  loading = false;
  error: string | null = null;

  readonly statuses: DealStatus[] = [
    'DISCOVERY',
    'CONTACTED',
    'NEGOTIATION',
    'DUE_DILIGENCE',
    'CLOSED',
    'REJECTED',
  ];

  readonly statusTitles: Record<DealStatus, string> = {
    DISCOVERY: 'Discovery',
    CONTACTED: 'Contacted',
    NEGOTIATION: 'Negotiation',
    DUE_DILIGENCE: 'Due diligence',
    CLOSED: 'Closed',
    REJECTED: 'Rejected',
  };

  private readonly statusOrder: DealStatus[] = this.statuses;

  columns: Record<DealStatus, DealCard[]> = this.emptyColumns();

  constructor(private dealService: DealsService) {}

  ngOnInit(): void {
    this.loadBoard();
  }

  loadBoard() {
    this.loading = true;
    this.error = null;

    console.log('Loading Kanban board for investorId:', this.investorId);
    
    this.dealService.getKanban(this.investorId).subscribe({
      next: (res) => {
        console.log('Kanban API response:', res);
        this.columns = this.normalizeBoard(res);
        console.log('Normalized columns:', this.columns);
        this.loading = false;
      },
      error: (err) => {
        console.error('Kanban API error:', err);
        this.error = 'Failed to load kanban board.';
        this.columns = this.emptyColumns();
        this.loading = false;
      }
    });
  }

  get connectedDropListIds(): string[] {
    return this.statuses.map((s) => `kanban-${s}`);
  }

  onColumnDropped(evt: KanbanDropEvent) {
    const newStatus = evt.status;

    // Explicit constraint: do not allow moving to ACCEPTED (even if backend ever adds it)
    if ((newStatus as unknown as string) === 'ACCEPTED') {
      return;
    }

    const dropEvent = evt.drop as CdkDragDrop<DealCard[]>;
    const movedDeal = dropEvent.item.data as DealCard;
    const fromIndex = this.statusOrder.indexOf(movedDeal.status);
    const toIndex = this.statusOrder.indexOf(newStatus);

    if (
      dropEvent.previousContainer !== dropEvent.container &&
      (movedDeal.status === 'CLOSED' || movedDeal.status === 'REJECTED')
    ) {
      this.error = 'Closed and rejected deals cannot change status.';
      this.loadBoard();
      return;
    }

    if (
      dropEvent.previousContainer !== dropEvent.container &&
      movedDeal.status === 'CLOSED' &&
      newStatus === 'REJECTED'
    ) {
      this.error = 'A closed deal cannot be rejected.';
      this.loadBoard();
      return;
    }

    if (dropEvent.previousContainer !== dropEvent.container && toIndex < fromIndex) {
      this.error = 'You cannot move a deal back to an earlier status.';
      this.loadBoard();
      return;
    }

    if (dropEvent.previousContainer === dropEvent.container) {
      // Reorder within same column
      if (dropEvent.previousIndex === dropEvent.currentIndex) return;
      moveItemInArray(dropEvent.container.data, dropEvent.previousIndex, dropEvent.currentIndex);
    } else {
      // Move between columns
      transferArrayItem(
        dropEvent.previousContainer.data,
        dropEvent.container.data,
        dropEvent.previousIndex,
        dropEvent.currentIndex
      );
    }

    const payload: MoveDealPayload = {
      dealId: movedDeal.id,
      newStatus,
      newColumnOrder: dropEvent.currentIndex,
    };

    this.dealService.moveCard(payload).subscribe({
      next: () => this.loadBoard(),
      error: () => this.loadBoard(),
    });
  }

  private emptyColumns(): Record<DealStatus, DealCard[]> {
    return this.statuses.reduce((acc, status) => {
      acc[status] = [];
      return acc;
    }, {} as Record<DealStatus, DealCard[]>);
  }

  private normalizeBoard(res: KanbanBoardResponse): Record<DealStatus, DealCard[]> {
    const base = this.emptyColumns();

    const fromColumns = res?.columns;
    if (fromColumns && typeof fromColumns === 'object') {
      for (const status of this.statuses) {
        const cards = fromColumns[status] ?? [];
        base[status] = this.sortCards(cards);
      }
      return base;
    }

    // Support `{ DISCOVERY: [...], CONTACTED: [...] }`
    let looksLikeStatusMap = true;
    for (const status of this.statuses) {
      if (!(status in (res as any))) {
        looksLikeStatusMap = false;
        break;
      }
    }
    if (looksLikeStatusMap) {
      for (const status of this.statuses) {
        base[status] = this.sortCards(((res as any)[status] ?? []) as DealCard[]);
      }
      return base;
    }

    // Support flat list `{ deals: [...] }`
    const deals = (res?.deals ?? []) as DealCard[];
    for (const deal of deals) {
      if (this.statuses.includes(deal.status)) {
        base[deal.status].push(deal);
      }
    }
    for (const status of this.statuses) {
      base[status] = this.sortCards(base[status]);
    }
    return base;
  }

  private sortCards(cards: DealCard[]): DealCard[] {
    return [...(cards ?? [])].sort((a, b) => {
      const ao = a.columnOrder ?? 0;
      const bo = b.columnOrder ?? 0;
      if (ao !== bo) return ao - bo;
      return String(a.id).localeCompare(String(b.id));
    }).map((card) => this.enrichCard(card));
  }

  private enrichCard(card: DealCard): DealCard {
    const startup = card.startupId ? STARTUP_CATALOG_BY_ID[card.startupId] : undefined;
    return {
      ...card,
      startupName: startup?.name ?? card.startupName ?? card.startupId,
      startupSector: startup?.sector ?? card.startupSector,
    };
  }
}
