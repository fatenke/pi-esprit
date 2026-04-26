import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { apiOrigin } from '../../../../core/api-origin';
import {
  DATA_ROOM_FOLDER_LABELS,
  DATA_ROOM_FOLDERS,
  DataRoomDocument,
  DataRoomFolder,
} from '../../models/data-room.models';
import { DataRoomApiService } from '../../services/data-room-api.service';

@Component({
  selector: 'app-data-room',
  templateUrl: './data-room.component.html',
  styleUrls: ['./data-room.component.css'],
})
export class DataRoomComponent implements OnInit, OnDestroy {
  roomId = '';
  loading = true;
  ndaBusy = false;
  uploadBusy = false;

  ndaSigned = false;
  allDocuments: DataRoomDocument[] = [];
  selectedFolder: DataRoomFolder = 'FINANCIAL';

  readonly folderLabels = DATA_ROOM_FOLDER_LABELS;
  readonly folders = DATA_ROOM_FOLDERS;

  /** Bonus analytics (session) */
  viewedDocumentIds = new Set<string>();
  lastViewed: { fileName: string; at: Date } | null = null;

  private sub = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private api: DataRoomApiService,
    private snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.sub.add(
      this.route.paramMap.subscribe((pm) => {
        const id = pm.get('roomId');
        if (id) {
          this.roomId = id;
          this.loadRoom();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  get folderCounts(): Record<DataRoomFolder, number> {
    const acc = {} as Record<DataRoomFolder, number>;
    for (const f of this.folders) acc[f] = 0;
    for (const d of this.allDocuments) {
      acc[d.folder] = (acc[d.folder] ?? 0) + 1;
    }
    return acc;
  }

  get filteredDocuments(): DataRoomDocument[] {
    return this.allDocuments.filter((d) => d.folder === this.selectedFolder);
  }

  get totalDocuments(): number {
    return this.allDocuments.length;
  }

  get viewedCount(): number {
    return this.viewedDocumentIds.size;
  }

  get selectedFolderLabel(): string {
    return this.folderLabels[this.selectedFolder];
  }

  loadRoom(silent = false): void {
    if (!this.roomId) return;
    if (!silent) {
      this.loading = true;
    }
    this.sub.add(
      this.api.getDataRoom(this.roomId).subscribe({
        next: (state) => {
          this.ndaSigned = state.ndaSigned;
          this.allDocuments = state.documents;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.snack.open('Impossible de charger la data room', 'Fermer', { duration: 4000 });
        },
      })
    );
  }

  onNdaSign(): void {
    this.ndaBusy = true;
    this.sub.add(
      this.api.signNda(this.roomId).subscribe({
        next: () => {
          this.ndaBusy = false;
          this.snack.open('NDA signe avec succes', 'OK', { duration: 3000 });
          this.loadRoom(true);
        },
        error: () => {
          this.ndaBusy = false;
          this.snack.open('Echec de la signature du NDA', 'Fermer', { duration: 4000 });
        },
      })
    );
  }

  onFolderChange(f: DataRoomFolder): void {
    this.selectedFolder = f;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.ndaSigned) return;

    this.uploadBusy = true;
    this.sub.add(
      this.api.upload(this.roomId, this.selectedFolder, file).subscribe({
        next: () => {
          this.uploadBusy = false;
          input.value = '';
          this.snack.open('Fichier televerse', 'OK', { duration: 2500 });
          this.loadRoom(true);
        },
        error: () => {
          this.uploadBusy = false;
          input.value = '';
          this.snack.open('Echec du televersement', 'Fermer', { duration: 4000 });
        },
      })
    );
  }

  onView(doc: DataRoomDocument): void {
    const url = this.viewUrl(doc);
    this.sub.add(
      this.api.logDocumentView(this.roomId, doc.id).subscribe({
        next: () => {
          this.viewedDocumentIds.add(doc.id);
          this.lastViewed = { fileName: doc.fileName, at: new Date() };
          window.open(url, '_blank', 'noopener,noreferrer');
        },
        error: () => {
          this.snack.open('La consultation n a pas pu etre journalisee (erreur reseau)', 'Fermer', { duration: 3000 });
          window.open(url, '_blank', 'noopener,noreferrer');
        },
      })
    );
  }

  onDownload(doc: DataRoomDocument): void {
    const url = this.downloadUrl(doc);
    const a = document.createElement('a');
    a.href = url;
    a.download = doc.fileName;
    a.rel = 'noopener';
    a.target = '_blank';
    a.click();
  }

  private viewUrl(doc: DataRoomDocument): string {
    if (doc.viewUrl) return doc.viewUrl;
    return `${apiOrigin()}/api/data-room/${encodeURIComponent(this.roomId)}/documents/${encodeURIComponent(doc.id)}/view`;
  }

  private downloadUrl(doc: DataRoomDocument): string {
    if (doc.downloadUrl) return doc.downloadUrl;
    return `${apiOrigin()}/api/data-room/${encodeURIComponent(this.roomId)}/documents/${encodeURIComponent(doc.id)}/download`;
  }
}
