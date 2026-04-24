import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { apiOrigin } from '../../../core/api-origin';
import {
  DATA_ROOM_FOLDERS,
  DataRoomDocument,
  DataRoomFolder,
  DataRoomState,
} from '../models/data-room.models';

/** Raw shapes tolerated from Spring Boot */
interface DataRoomDocumentDto {
  id?: string;
  fileName?: string;
  name?: string;
  type?: string;
  mimeType?: string;
  documentType?: string;
  uploadDate?: string;
  uploadedAt?: string;
  createdAt?: string;
  folder?: string;
  category?: string;
  viewUrl?: string;
  downloadUrl?: string;
}

interface DataRoomApiEnvelope {
  roomId?: string;
  id?: string;
  ndaSigned?: boolean;
  nda?: { signed?: boolean };
  documents?: DataRoomDocumentDto[];
  items?: DataRoomDocumentDto[];
  folders?: Partial<Record<string, DataRoomDocumentDto[]>>;
  [key: string]: unknown;
}

@Injectable({
  providedIn: 'root',
})
export class DataRoomApiService {
  private readonly base = `${apiOrigin()}/api/data-room`;
  private readonly ndaBase = `${apiOrigin()}/api/nda`;
  private readonly logBase = `${apiOrigin()}/api/log`;

  constructor(private http: HttpClient) {}

  getDataRoom(roomId: string): Observable<DataRoomState> {
    return this.http
      .get<DataRoomApiEnvelope>(`${this.base}/${encodeURIComponent(roomId)}`)
      .pipe(map((raw) => this.normalize(roomId, raw)));
  }

  upload(roomId: string, folder: DataRoomFolder, file: File): Observable<unknown> {
    const fd = new FormData();
    fd.append('file', file, file.name);
    fd.append('roomId', roomId);
    fd.append('folder', folder);
    return this.http.post(`${this.base}/upload`, fd);
  }

  signNda(roomId: string): Observable<unknown> {
    return this.http.post(`${this.ndaBase}/sign`, { roomId });
  }

  logDocumentView(roomId: string, documentId: string): Observable<unknown> {
    return this.http.post(`${this.logBase}/view`, { roomId, documentId });
  }

  private normalize(roomId: string, raw: DataRoomApiEnvelope): DataRoomState {
    const ndaSigned: boolean =
      raw.ndaSigned === true ||
      !!(
        raw.nda &&
        typeof raw.nda === 'object' &&
        raw.nda.signed === true
      );

    const documents: DataRoomDocument[] = [];

    if (Array.isArray(raw.documents)) {
      for (const d of raw.documents) {
        const doc = this.mapDoc(d);
        if (doc) documents.push(doc);
      }
    } else if (Array.isArray(raw.items)) {
      for (const d of raw.items) {
        const doc = this.mapDoc(d);
        if (doc) documents.push(doc);
      }
    } else if (raw.folders && typeof raw.folders === 'object') {
      for (const folder of DATA_ROOM_FOLDERS) {
        const list = raw.folders[folder] ?? raw.folders[folder.toLowerCase()];
        if (!Array.isArray(list)) continue;
        for (const d of list) {
          const doc = this.mapDoc(d, folder);
          if (doc) documents.push(doc);
        }
      }
    }

    return {
      roomId: String(raw.roomId ?? raw.id ?? roomId),
      ndaSigned,
      documents,
    };
  }

  private mapDoc(d: DataRoomDocumentDto, defaultFolder?: DataRoomFolder): DataRoomDocument | null {
    const id = d.id != null ? String(d.id) : '';
    const fileName = d.fileName ?? d.name ?? '';
    if (!id || !fileName) return null;

    const folder =
      this.parseFolder(d.folder ?? d.category, defaultFolder) ?? 'FINANCIAL';

    const type = d.type ?? d.documentType ?? d.mimeType ?? '—';
    const uploadDate =
      d.uploadDate ?? d.uploadedAt ?? d.createdAt ?? new Date().toISOString();

    return {
      id,
      fileName,
      type,
      uploadDate,
      folder,
      viewUrl: d.viewUrl,
      downloadUrl: d.downloadUrl,
    };
  }

  private parseFolder(
    value: string | undefined,
    fallback?: DataRoomFolder
  ): DataRoomFolder | null {
    if (!value) return fallback ?? null;
    const v = value.toUpperCase().replace(/\s+/g, '_');
    const map: Record<string, DataRoomFolder> = {
      FINANCIAL: 'FINANCIAL',
      LEGAL: 'LEGAL',
      PRODUCT: 'PRODUCT',
      TEAM: 'TEAM',
      MARKET: 'MARKET',
    };
    return map[v] ?? fallback ?? null;
  }
}
