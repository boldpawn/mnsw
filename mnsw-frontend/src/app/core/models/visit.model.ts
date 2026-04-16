import { FormalityStatus, FormalityType } from './formality.model';

export interface Visit {
  id: string;
  imoNumber: string;
  vesselName: string;
  vesselFlag?: string;
  portLocode: string;
  eta?: string; // ISO 8601
  etd?: string; // ISO 8601
  formalities?: FormalitySummary[];
}

export interface FormalitySummary {
  id: string;
  type: FormalityType;
  version: number;
  status: FormalityStatus;
  submittedAt: string;
}
