import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Formality,
  FormalityPayload,
  FormalityStatus,
  FormalityType,
  Page,
} from '../models/formality.model';

export interface FormalityFilters {
  type?: FormalityType;
  status?: FormalityStatus;
  visitId?: string;
  portLocode?: string;
  fromDate?: string;
  toDate?: string;
  includeSuperseded?: boolean;
  page?: number;
  size?: number;
}

export interface SubmitFormalityRequest {
  visitId: string;
  type: FormalityType;
  lrn?: string;
  messageIdentifier?: string;
  payload: FormalityPayload;
}

export interface CorrectFormalityRequest {
  lrn?: string;
  messageIdentifier?: string;
  payload: FormalityPayload;
}

export interface SubmitFormalityResult {
  formalityId: string;
  messageIdentifier: string;
  status: FormalityStatus;
  statusUrl: string;
  version?: number;
  previousVersionId?: string;
}

export interface RejectRequest {
  reasonCode: string;
  reasonDescription: string;
}

@Injectable({ providedIn: 'root' })
export class FormalityService {
  private readonly baseUrl = `${environment.apiUrl}/formalities`;

  constructor(private http: HttpClient) {}

  list(filters: FormalityFilters = {}): Observable<Page<Formality>> {
    let params = new HttpParams();

    if (filters.type) params = params.set('type', filters.type);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.visitId) params = params.set('visitId', filters.visitId);
    if (filters.portLocode) params = params.set('portLocode', filters.portLocode);
    if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters.toDate) params = params.set('toDate', filters.toDate);
    if (filters.includeSuperseded !== undefined)
      params = params.set('includeSuperseded', String(filters.includeSuperseded));
    if (filters.page !== undefined) params = params.set('page', String(filters.page));
    if (filters.size !== undefined) params = params.set('size', String(filters.size));

    return this.http.get<Page<Formality>>(this.baseUrl, { params });
  }

  get(id: string): Observable<Formality> {
    return this.http.get<Formality>(`${this.baseUrl}/${id}`);
  }

  submit(command: SubmitFormalityRequest): Observable<SubmitFormalityResult> {
    return this.http.post<SubmitFormalityResult>(this.baseUrl, command);
  }

  correct(id: string, command: CorrectFormalityRequest): Observable<SubmitFormalityResult> {
    return this.http.post<SubmitFormalityResult>(`${this.baseUrl}/${id}/corrections`, command);
  }

  approve(id: string): Observable<Formality> {
    return this.http.put<Formality>(`${this.baseUrl}/${id}/approve`, {});
  }

  reject(id: string, request: RejectRequest): Observable<Formality> {
    return this.http.put<Formality>(`${this.baseUrl}/${id}/reject`, request);
  }

  setUnderReview(id: string): Observable<Formality> {
    return this.http.put<Formality>(`${this.baseUrl}/${id}/review`, {});
  }
}
