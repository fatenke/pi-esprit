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
}
