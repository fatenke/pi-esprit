import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
  DATA_ROOM_FOLDER_LABELS,
  DATA_ROOM_FOLDERS,
  DataRoomFolder,
} from '../../models/data-room.models';

@Component({
  selector: 'app-folder-list',
  templateUrl: './folder-list.component.html',
  styleUrls: ['./folder-list.component.css'],
})
export class FolderListComponent {
  readonly folders = DATA_ROOM_FOLDERS;
  readonly labels = DATA_ROOM_FOLDER_LABELS;

  @Input({ required: true }) selected!: DataRoomFolder;
  @Input() counts: Record<DataRoomFolder, number> | null = null;

  @Output() selectedChange = new EventEmitter<DataRoomFolder>();

  select(f: DataRoomFolder) {
    this.selectedChange.emit(f);
  }

  countFor(f: DataRoomFolder): number {
    return this.counts?.[f] ?? 0;
  }

  iconFor(f: DataRoomFolder): string {
    switch (f) {
      case 'FINANCIAL':
        return 'monitoring';
      case 'LEGAL':
        return 'gavel';
      case 'PRODUCT':
        return 'deployed_code';
      case 'TEAM':
        return 'groups';
      case 'MARKET':
        return 'travel_explore';
      default:
        return 'folder';
    }
  }

  hintFor(f: DataRoomFolder): string {
    switch (f) {
      case 'FINANCIAL':
        return 'Bilans, projections et KPIs';
      case 'LEGAL':
        return 'Contrats, statuts et conformite';
      case 'PRODUCT':
        return 'Roadmap, demos et specs';
      case 'TEAM':
        return 'Organisation et recrutement';
      case 'MARKET':
        return 'Etudes, traction et concurrence';
      default:
        return '';
    }
  }
}
