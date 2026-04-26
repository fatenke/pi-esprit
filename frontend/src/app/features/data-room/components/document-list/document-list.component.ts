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

  formatDate(iso: string): string {
    const d = new Date(iso);
    return isNaN(d.getTime()) ? iso : d.toLocaleString('fr-TN');
  }

  iconFor(type: string): string {
    const normalized = (type || '').toLowerCase();
    if (normalized.includes('pdf')) return 'picture_as_pdf';
    if (normalized.includes('sheet') || normalized.includes('excel') || normalized.includes('csv')) return 'table_chart';
    if (normalized.includes('image') || normalized.includes('png') || normalized.includes('jpg')) return 'image';
    if (normalized.includes('presentation') || normalized.includes('powerpoint')) return 'slideshow';
    return 'description';
  }

  toneFor(type: string): string {
    const normalized = (type || '').toLowerCase();
    if (normalized.includes('pdf')) return 'pdf';
    if (normalized.includes('sheet') || normalized.includes('excel') || normalized.includes('csv')) return 'sheet';
    if (normalized.includes('image') || normalized.includes('png') || normalized.includes('jpg')) return 'image';
    return 'default';
  }
}
