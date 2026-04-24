export type DataRoomFolder =
  | 'FINANCIAL'
  | 'LEGAL'
  | 'PRODUCT'
  | 'TEAM'
  | 'MARKET';

export const DATA_ROOM_FOLDERS: DataRoomFolder[] = [
  'FINANCIAL',
  'LEGAL',
  'PRODUCT',
  'TEAM',
  'MARKET',
];

export const DATA_ROOM_FOLDER_LABELS: Record<DataRoomFolder, string> = {
  FINANCIAL: 'Financial',
  LEGAL: 'Legal',
  PRODUCT: 'Product',
  TEAM: 'Team',
  MARKET: 'Market',
};

export interface DataRoomDocument {
  id: string;
  fileName: string;
  type: string;
  uploadDate: string;
  folder: DataRoomFolder;
  /** Optional direct URLs from backend */
  viewUrl?: string;
  downloadUrl?: string;
}

export interface DataRoomState {
  roomId: string;
  ndaSigned: boolean;
  documents: DataRoomDocument[];
}
