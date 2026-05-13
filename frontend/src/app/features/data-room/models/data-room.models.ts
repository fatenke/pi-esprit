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
  FINANCIAL: 'Financier',
  LEGAL: 'Juridique',
  PRODUCT: 'Produit',
  TEAM: 'Equipe',
  MARKET: 'Marche',
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

export type NdaStatus = 'PENDING' | 'SIGNED' | 'REJECTED';
export type SignatureType = 'DRAWN';

export interface NdaAgreement {
  id: string;
  dataRoomId: string;
  status: NdaStatus;
  ndaContent: string;
  ndaHash: string;
  signedAt?: string | null;
}

export interface NdaStatusResponse {
  dataRoomId: string;
  status: NdaStatus;
  canAccessDataRoom: boolean;
  signedAt?: string | null;
}

export interface SignNdaPayload {
  signerFullName: string;
  signatureType: SignatureType;
  signatureImageBase64: string | null;
  typedSignature: string | null;
  acceptedTerms: boolean;
}

export interface NdaSignResponse {
  ndaId: string;
  status: NdaStatus;
  signedAt: string;
  signatureHash: string;
  certificateUrl?: string;
  message: string;
}
