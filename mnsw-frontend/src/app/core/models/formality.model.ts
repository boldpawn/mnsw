export type FormalityType = 'NOA' | 'NOS' | 'NOD' | 'VID' | 'SID';
export type FormalityStatus = 'SUBMITTED' | 'ACCEPTED' | 'REJECTED' | 'UNDER_REVIEW' | 'SUPERSEDED';
export type SubmissionChannel = 'WEB' | 'RIM';

export interface Formality {
  id: string;
  visitId: string;
  type: FormalityType;
  version: number;
  status: FormalityStatus;
  submitterId: string;
  submitterName?: string;
  lrn?: string;
  messageIdentifier: string;
  submittedAt: string; // ISO 8601
  channel: SubmissionChannel;
  payload?: FormalityPayload;
  frmResponse?: FrmResponse;
  versionHistory?: FormalityVersionSummary[];
  vessel?: {
    imoNumber: string;
    vesselName: string;
    portLocode: string;
  };
}

export interface FrmResponse {
  status: 'ACCEPTED' | 'REJECTED' | 'UNDER_REVIEW';
  reasonCode?: string;
  reasonDescription?: string;
  sentAt?: string;
}

export interface FormalityVersionSummary {
  id: string;
  version: number;
  status: FormalityStatus;
  submittedAt: string;
}

export interface NoaPayload {
  expectedArrival: string;
  lastPortLocode?: string;
  nextPortLocode?: string;
  purposeOfCall?: string;
  personsOnBoard?: number;
  dangerousGoods?: boolean;
  wasteDelivery?: boolean;
  maxStaticDraught?: number;
}

export interface NosPayload {
  actualSailing: string;
  nextPortLocode?: string;
  destinationCountry?: string;
}

export interface NodPayload {
  expectedDeparture: string;
  nextPortLocode?: string;
  destinationCountry?: string;
  lastCargoOperations?: string;
}

export interface VidPayload {
  certificateNationality?: string;
  grossTonnage?: number;
  netTonnage?: number;
  deadweight?: number;
  lengthOverall?: number;
  shipType?: string;
  callSign?: string;
  mmsi?: string;
}

export interface SidPayload {
  ispsLevel: 1 | 2 | 3;
  last10Ports?: PortCall[];
  securityDeclaration?: string;
  shipToShipActivities?: boolean;
  designatedAuthority?: string;
  ssasActivated?: boolean;
}

export interface PortCall {
  locode: string;
  arrival?: string;
  departure?: string;
  ispsLevel?: number;
}

export type FormalityPayload = NoaPayload | NosPayload | NodPayload | VidPayload | SidPayload;

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
