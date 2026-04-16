import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User, CreateUserRequest, UpdateUserRequest } from '../models/user.model';
import { Page } from '../models/formality.model';

export interface UserFilters {
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly baseUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  list(filters: UserFilters = {}): Observable<Page<User>> {
    let params = new HttpParams();

    if (filters.page !== undefined) params = params.set('page', String(filters.page));
    if (filters.size !== undefined) params = params.set('size', String(filters.size));

    return this.http.get<Page<User>>(this.baseUrl, { params });
  }

  get(id: string): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateUserRequest): Observable<User> {
    return this.http.post<User>(this.baseUrl, request);
  }

  update(id: string, request: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
