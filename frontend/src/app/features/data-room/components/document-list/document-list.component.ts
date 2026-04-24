import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DataRoomDocument } from '../../models/data-room.models';

@Component({
  selector: 'app-document-list',
  templateUrl: './document-list.component.html',
  styleUrls: ['./document-list.component.css'],
})
export class DocumentListComponent {
  @Input() documents: DataRoomDocument[] = [];
  @Input() loading = false;

  @Output() view = new EventEmitter<DataRoomDocument>();
  @Output() download = new EventEmitter<DataRoomDocument>();

  displayedColumns: string[] = ['fileName', 'type', 'uploadDate', 'actions'];

  formatDate(iso: string): string {
    const d = new Date(iso);
    return isNaN(d.getTime()) ? iso : d.toLocaleString();
  }
}
