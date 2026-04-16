import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User, UserRole, AuthResult } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'mnsw_token';
  private readonly USER_KEY = 'mnsw_user';

  private _currentUser = signal<User | null>(this.loadUser());
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, password: string): Observable<AuthResult> {
    return this.http
      .post<AuthResult>(`${environment.apiUrl}/auth/login`, { email, password })
      .pipe(
        tap(result => {
          localStorage.setItem(this.TOKEN_KEY, result.token);
          localStorage.setItem(this.USER_KEY, JSON.stringify(result.user));
          this._currentUser.set(result.user);
        })
      );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  hasRole(role: UserRole): boolean {
    return this._currentUser()?.role === role;
  }

  hasAnyRole(...roles: UserRole[]): boolean {
    const role = this._currentUser()?.role;
    return !!role && roles.includes(role);
  }

  private loadUser(): User | null {
    try {
      const stored = localStorage.getItem(this.USER_KEY);
      return stored ? (JSON.parse(stored) as User) : null;
    } catch {
      return null;
    }
  }
}
