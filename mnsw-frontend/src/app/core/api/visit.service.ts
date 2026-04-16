import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Visit } from '../models/visit.model';
import { Page } from '../models/formality.model';

export interface VisitFilters {
  portLocode?: string;
  imoNumber?: string;
  status?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class VisitService {
  private readonly baseUrl = `${environment.apiUrl}/visits`;

  constructor(private http: HttpClient) {}

  list(filters: VisitFilters = {}): Observable<Page<Visit>> {
    let params = new HttpParams();

    if (filters.portLocode) params = params.set('portLocode', filters.portLocode);
    if (filters.imoNumber) params = params.set('imoNumber', filters.imoNumber);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.page !== undefined) params = params.set('page', String(filters.page));
    if (filters.size !== undefined) params = params.set('size', String(filters.size));

    return this.http.get<Page<Visit>>(this.baseUrl, { params });
  }

  get(id: string): Observable<Visit> {
    return this.http.get<Visit>(`${this.baseUrl}/${id}`);
  }
}
